package org.terasology.gestalt.dependencyinjection;

import com.google.auto.service.AutoService;
import com.google.common.base.CaseFormat;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import org.terasology.gestalt.util.collection.TypeKeyedMap;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.inject.Inject;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * An AnnotationProcessor for generating Provider implementations from {@link GameLogicProvider} annotated interfaces.
 * The implementation has these features:
 * <ul>
 *     <li>A constructor taking a TypeKeyedMap to supply other providers, which it will install itself into</li>
 *     <li>An implementation of each method that construct an instance of the returned object on first call, and subsequently return it</li>
 * </ul>
 */
@SupportedAnnotationTypes({"org.terasology.gestalt.dependencyinjection.GameLogicProvider"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class DependencyInjectionAnnotationProcessor extends AbstractProcessor {

    private static final String IMPL_SUFFIX = "Impl";
    private static final TypeToken<TypeKeyedMap<Object>> PROVIDER_MAP_TYPE = new TypeToken<TypeKeyedMap<Object>>() {
    };
    private static final TypeToken<List<Object>> SYSTEM_LIST_TYPE = new TypeToken<List<Object>>() {
    };

    private static final TypeToken<Class<?>> CLASS_TYPE = new TypeToken<Class<?>>() {
    };


    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment environment) {
        for (TypeElement annotation : annotations) {
            Set<? extends Element> annotatedElements
                    = environment.getElementsAnnotatedWith(annotation);
            Map<Boolean, List<Element>> annotatedTypes = annotatedElements.stream().collect(Collectors.partitioningBy(element -> element.getKind() == ElementKind.INTERFACE));

            for (Element element : annotatedTypes.get(true)) {
                if (element.getAnnotation(DoNotGenerate.class) == null) {
                    new ProviderImplBuilder((TypeElement) element).generate();
                }
            }

            for (Element element : annotatedTypes.get(false)) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "@GameLogicProvider must be applied to interfaces", element);
            }
        }
        return false;
    }

    private static class MethodRef {
        private String variableName;
        private String method;

        MethodRef(String variableName, String method) {
            this.variableName = variableName;
            this.method = method;
        }
    }

    private class ProviderImplBuilder {
        private final TypeElement providerType;
        private final List<FieldSpec> fields = new ArrayList<>();
        private final List<MethodSpec> methods = new ArrayList<>();
        private final Map<TypeMirror, MethodRef> typeToProviderMap = new HashMap<>();

        ProviderImplBuilder(TypeElement providerType) {
            this.providerType = providerType;
        }

        void generate() {
            try {
                String providerQualifiedName = providerType.getQualifiedName().toString();
                String providerPackage = providerQualifiedName.substring(0, providerQualifiedName.lastIndexOf('.'));
                String providerName = providerQualifiedName.substring(providerQualifiedName.lastIndexOf('.') + 1);
                String providerBuilderName = providerName + IMPL_SUFFIX;

                indexProvidedTypes(providerType, "this");

                List<TypeElement> dependencies = getDependencies(providerType);
                for (TypeElement dependency : dependencies) {
                    String dependencyVariable = getVariableNameForProvider(dependency);
                    indexProvidedTypes(dependency, dependencyVariable);
                    fields.add(FieldSpec.builder(ClassName.get(dependency), dependencyVariable, Modifier.PRIVATE).build());
                }

                generateInitMethod(dependencies);

                generateProviderForMethod();

                List<ExecutableElement> getterMethods = providerType.getEnclosedElements().stream().filter(e -> e.getKind() == ElementKind.METHOD).map(x -> (ExecutableElement) x).collect(Collectors.toList());
                for (ExecutableElement methodDef : getterMethods) {
                    generateFactoryMethod(methodDef);
                }
                generateGetAllProvidersMethod(getterMethods);

                writeFile(providerPackage, providerBuilderName);
            } catch (GenerationException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage(), providerType);
            }
        }

        private void generateGetAllProvidersMethod(List<ExecutableElement> getterMethods) {
            MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("getAllSystems")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .returns(SYSTEM_LIST_TYPE.getType())
                    .addStatement("$T $N = new $T<>()", SYSTEM_LIST_TYPE.getType(), "result", ArrayList.class);
            for (ExecutableElement getterMethod : getterMethods) {
                methodBuilder.addStatement("$N.add($N())", "result", getterMethod.getSimpleName());
            }
            methodBuilder.addStatement("return $N", "result");
            methods.add(methodBuilder.build());
        }

        private void generateProviderForMethod() throws GenerationException {
            MethodSpec.Builder builder = MethodSpec.methodBuilder("providerFor")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .returns(CLASS_TYPE.getType())
                    .addStatement("return $T.class", providerType);
            methods.add(builder.build());
        }

        private void generateFactoryMethod(ExecutableElement methodDef) throws GenerationException {
            MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodDef.getSimpleName().toString())
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .returns(TypeName.get(methodDef.getReturnType()));

            TypeElement returnType = getReturnType(methodDef);
            ExecutableElement constructor = findConstructor(returnType);

            List<Object> arguments = Lists.newArrayList();
            arguments.add(methodDef.getSimpleName().toString());
            arguments.add(TypeName.get(methodDef.getReturnType()));
            for (VariableElement parameter : constructor.getParameters()) {
                MethodRef methodRef = typeToProviderMap.get(parameter.asType());
                if (methodRef == null) {
                    throw new GenerationException("No provider for " + parameter.asType());
                } else {
                    arguments.add(methodRef.variableName);
                    arguments.add(methodRef.method);
                }
            }

            methodBuilder.beginControlFlow("if ($N == null)", methodDef.getSimpleName().toString());
            String statement = "this.$N = new $T(" +
                    constructor.getParameters().stream().map(x -> "$N.$N()").collect(Collectors.joining(", ")) +
                    ")";
            methodBuilder.addStatement(statement, arguments.toArray(new Object[]{}));
            methodBuilder.endControlFlow();
            methodBuilder.addStatement("return $N", methodDef.getSimpleName().toString());

            methods.add(methodBuilder.build());
            fields.add(FieldSpec.builder(TypeName.get(methodDef.getReturnType()), methodDef.getSimpleName().toString(), Modifier.PRIVATE).build());
        }

        private ExecutableElement findConstructor(TypeElement returnType) throws GenerationException {
            List<ExecutableElement> constructors = returnType.getEnclosedElements().stream().filter(x -> x.getKind() == ElementKind.CONSTRUCTOR).map(x -> (ExecutableElement) x).sorted(Comparator.comparingInt(this::scoreConstructor)).collect(Collectors.toList());
            switch (constructors.size()) {
                case 0:
                    throw new GenerationException("No viable constructors found for type " + returnType.getQualifiedName().toString());
                case 1:
                    return constructors.get(0);
                default:
                    ExecutableElement proposedConstructor = constructors.get(0);
                    ExecutableElement runnerUpConstructor = constructors.get(1);
                    if (scoreConstructor(proposedConstructor) == scoreConstructor(runnerUpConstructor)) {
                        throw new GenerationException("Unable to select between constructors for " + returnType.getQualifiedName().toString() + " - use the Inject annotation on a single constructor to indicate which should be used");
                    }
                    return proposedConstructor;
            }
        }

        private int scoreConstructor(ExecutableElement a) {
            boolean injectAnnotated = AnnotationProcessorUtil.getAnnotation(a, Inject.class) != null;
            if (injectAnnotated && a.getParameters().size() == 0) {
                return 0;
            } else if (injectAnnotated) {
                return 1;
            }
            if (a.getParameters().size() == 0) {
                return 2;
            } else {
                return 3;
            }
        }

        private TypeElement getReturnType(ExecutableElement methodDef) throws GenerationException {
            Element returnElement = processingEnv.getTypeUtils().asElement(methodDef.getReturnType());
            if (returnElement.getKind() != ElementKind.CLASS) {
                throw new GenerationException("Provider methods must return a class - " + returnElement.getKind() + " is not.");
            }
            TypeElement returnType = (TypeElement) returnElement;
            if (returnType.getModifiers().contains(Modifier.ABSTRACT)) {
                throw new GenerationException("Provider methods must return a non-abstract class - " + returnElement.getKind() + " is not.");
            }
            return returnType;
        }

        private void generateInitMethod(List<TypeElement> dependencies) {
            MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("init")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .addParameter(PROVIDER_MAP_TYPE.getType(), "providers");
            for (TypeElement dependency : dependencies) {
                String variableName = getVariableNameForProvider(dependency);
                methodBuilder.addStatement("$N = providers.get($T.class)", variableName, dependency);
            }
            methods.add(methodBuilder.build());
        }

        private String getVariableNameForProvider(TypeElement provider) {
            return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, provider.getSimpleName().toString());
        }

        private List<TypeElement> getDependencies(TypeElement providerType) {
            List<TypeMirror> dependsOn = AnnotationProcessorUtil.getAnnotationClassList(providerType, GameLogicProvider.class, "dependsOn");
            Map<Boolean, List<Element>> dependenciesSplitByValidity = dependsOn.stream().map(processingEnv.getTypeUtils()::asElement).collect(Collectors.partitioningBy(element -> element.getKind() == ElementKind.INTERFACE));
            for (Element element : dependenciesSplitByValidity.get(false)) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "@GameLogicProvider dependencies must be interfaces - " + element.getSimpleName() + "is not.", providerType);
            }
            return dependenciesSplitByValidity.get(true).stream().map(e -> (TypeElement) e).collect(Collectors.toList());
        }

        private void indexProvidedTypes(TypeElement providingType, String typeVariable) {
            for (Element enclosedDependencyElement : providingType.getEnclosedElements()) {
                if (enclosedDependencyElement.getKind() == ElementKind.METHOD) {
                    ExecutableElement methodDef = (ExecutableElement) enclosedDependencyElement;
                    typeToProviderMap.put(methodDef.getReturnType(), new MethodRef(typeVariable, methodDef.getSimpleName().toString()));
                }
            }
        }

        private void writeFile(String providerPackage, String providerBuilderName) {
            TypeSpec providerBuilder = TypeSpec.classBuilder(providerBuilderName)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addSuperinterface(Provider.class)
                    .addSuperinterface(providerType.asType())
                    .addFields(fields)
                    .addMethods(methods).build();

            try {
                JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(providerPackage + "." + providerBuilderName);
                try (Writer out = sourceFile.openWriter()) {
                    JavaFile.builder(providerPackage, providerBuilder).build().writeTo(out);
                }
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to create builder file - " + e.getMessage());
            }
        }
    }
}
