// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.gestalt.annotation.processing;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedOptions;
import javax.inject.Inject;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementScanner8;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


@SupportedOptions({"org.terasology.gestalt.annotation.processing"})
public class BeanDefinitionProcessor extends AbstractProcessor {

    private static final String SERVICE_CLASS = "org.terasology.context.annotation.Service";
    private static final String SCOPED_CLASS = "org.terasology.context.annotation.Scoped";
    private static final String TRANSIENT_CLASS = "org.terasology.context.annotation.Transient";


    private static final String[] TARGET_ANNOTATIONS = new String[]{
            "javax.inject.Inject",
            "javax.inject.Qualifier",
            "javax.inject.Singleton",
            SERVICE_CLASS,
            SCOPED_CLASS,
            TRANSIENT_CLASS
    };
    private final Set<String> processed = new HashSet<>();
    private final Set<String> beanDefinitions = new HashSet<>();
    protected Elements elementUtils;
    protected Types typeUtils;
    private Filer filer;
    private ServiceTypeWriter writer;
    private ElementUtility utility;
    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        this.filer = processingEnvironment.getFiler();
        this.writer = new ServiceTypeWriter(this.filer);
        this.messager = processingEnvironment.getMessager();

        this.elementUtils = processingEnvironment.getElementUtils();
        this.typeUtils = processingEnv.getTypeUtils();

        this.utility = new ElementUtility(processingEnvironment.getElementUtils(), processingEnvironment.getTypeUtils());
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton("*");
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment environment) {

        Set<? extends TypeElement> filteredAnnotation = annotations.stream()
                .filter(target -> {
                    for (Element encElement : target.getEnclosedElements()) {
                        if (encElement instanceof ExecutableElement || encElement instanceof VariableElement) {
                            if (utility.hasStereotype(encElement, Arrays.asList(TARGET_ANNOTATIONS))) {
                                return true;
                            }
                        }

                    }
                    return utility.hasStereotype(target, Arrays.asList(TARGET_ANNOTATIONS));
                })
                .collect(Collectors.toSet());


        filteredAnnotation.forEach(annotation -> environment.getElementsAnnotatedWith(annotation)
                .stream().filter(element -> element.getKind() != ElementKind.ANNOTATION_TYPE)
                .forEach(element -> {
                    TypeElement typeElement = utility.classElementFor(element);
                    if (element.getKind() == ElementKind.ENUM) {
                        return;
                    }
                    if (typeElement == null) {
                        return;
                    }
                    String name = typeElement.getQualifiedName().toString();
                    if (!beanDefinitions.contains(name)
                            && !processed.contains(name)
                            && typeElement.getKind() != ElementKind.INTERFACE) {
                        beanDefinitions.add(name);
                    }
                }));

        for (String name : processed) {
            beanDefinitions.remove(name);
        }

        int count = beanDefinitions.size();
        if (count > 0) {
            this.messager.printMessage(Diagnostic.Kind.NOTE, String.format("Creating bean classes for %s type elements", count));
            beanDefinitions.forEach(className -> {
                if (processed.add(className)) {
                    final TypeElement result = utility.getElements().getTypeElement(className);
                    DefinitionWriter visitor = new DefinitionWriter(result);
                    result.accept(visitor, className);
                }
            });
        }


        if (environment.processingOver()) {
            try {
                this.writer.finish();
            } catch (IOException e) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Cannot write BeanDefinitions: " + e.getMessage());
            }
        }
        return false;
    }

    public class DefinitionWriter extends ElementScanner8<Object, String> {
        public static final String BASE_PACKAGE = "org.terasology.context";
        public static final String ARGUMENT_FIELD = "$ARGUMENT";
        public static final String CLASS_METADATA_FIELD = "$CLASS_METADATA";
        private final TypeElement concreteClass;
        private final List<CodeBlock> arguments = new ArrayList<>();


        DefinitionWriter(TypeElement concreteClass) {
            this.concreteClass = concreteClass;
        }

        @Override
        public Object visitType(TypeElement e, String className) {
            if (!e.equals(this.concreteClass)) {
                return super.visitType(e, className);
            }
            writeBeanDefinition(e, className);
            return super.visitType(e, className);
        }

        private Object getValue(Object target) {
            Object result;
            if (target instanceof String) {
                result = String.format("\"%s\"", target);
            } else {
                result = target;
            }
            return result;

        }

        private List<CodeBlock> buildAnnotationValues(List<? extends AnnotationMirror> mirrors) {
            List<CodeBlock> annotationsBuilder = new ArrayList<>();
            for (AnnotationMirror ann : mirrors) {
                DeclaredType declaredType = ann.getAnnotationType();

                List<CodeBlock> defaults = new ArrayList<>();
                List<CodeBlock> values = new ArrayList<>();
                for (Element element : declaredType.asElement().getEnclosedElements()) {
                    if (element.getKind() == ElementKind.METHOD) {
                        ExecutableElement executableElement = (ExecutableElement) element;
                        AnnotationValue value = executableElement.getDefaultValue();
                        if (value != null) {
                            defaults.add(CodeBlock.of("$S,$L", executableElement.getSimpleName(), getValue(value.getValue())));
                        }
                    }
                }

                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : ann.getElementValues().entrySet()) {
                    ExecutableElement executableElement = entry.getKey();
                    AnnotationValue value = entry.getValue();
                    if (value != null) {
                        values.add(CodeBlock.of("$S,$L", executableElement.getSimpleName(), getValue(value.getValue())));
                    }
                }

                List<? extends AnnotationMirror> children = declaredType.asElement().getAnnotationMirrors().stream().filter(k -> {
                    String name = k.getAnnotationType().toString();
                    return !name.startsWith("java.lang");
                }).collect(Collectors.toList());

                annotationsBuilder.add(CodeBlock.builder().add("new $T($T.class,$S,$T.of($L),$T.of($L),new $T[] {$L})",
                        ClassName.get(BASE_PACKAGE, "DefaultAnnotationValue"),
                        declaredType,
                        declaredType.toString(),
                        ClassName.get(BASE_PACKAGE, "AnnotationValue"),
                        CodeBlock.join(defaults, ","),
                        ClassName.get(BASE_PACKAGE, "AnnotationValue"),
                        CodeBlock.join(values, ","),
                        ClassName.get(BASE_PACKAGE, "AnnotationValue"),
                        CodeBlock.join(buildAnnotationValues(children), ",")).build());
            }
            return annotationsBuilder;
        }

        private CodeBlock buildAnnotationMetadataBlock(Element element) {
            List<CodeBlock> blocks = buildAnnotationValues(element.getAnnotationMirrors());
            return CodeBlock.builder().add("new $T(new $T[]{$L})",
                    ClassName.get(BASE_PACKAGE, "DefaultAnnotationMetadata"),
                    ClassName.get(BASE_PACKAGE, "AnnotationValue"),
                    CodeBlock.join(blocks, ",")).build();

        }

        private CodeBlock buildArgument(VariableElement element) {
            TypeMirror type = element.asType();
            if (type instanceof DeclaredType && !((DeclaredType) type).getTypeArguments().isEmpty()) {
                DeclaredType declaredType = (DeclaredType) type; //GENERIC!
                if (((DeclaredType) type).getTypeArguments().size() == 1) {
                    arguments.add(CodeBlock.builder().add("new $T($T.class,$T.class,$L)",
                            ClassName.get(BASE_PACKAGE, "SingleGenericArgument"),
                            processingEnv.getTypeUtils().erasure(element.asType()),
                            declaredType.getTypeArguments().get(0),
                            buildAnnotationMetadataBlock(element)).build());
                    return CodeBlock.builder().add("$L[" + (arguments.size() - 1) + "]", ARGUMENT_FIELD).build();
                }
                return null;
            } else {
                arguments.add(CodeBlock.builder().add("new $T($T.class,$L)",
                        ClassName.get(BASE_PACKAGE, "DefaultArgument"),
                        element,
                        buildAnnotationMetadataBlock(element)).build());
                return CodeBlock.builder().add("$L[" + (arguments.size() - 1) + "]", ARGUMENT_FIELD).build();
            }
        }

        private CodeBlock buildObjectPointCreationBlock(TypeElement target) {
            CodeBlock.Builder builder = CodeBlock.builder();
            ClassName targetClass = ClassName.get(target);

            // abstract/interface classes can't be instantiated
            if(target.getModifiers().contains(Modifier.ABSTRACT) || target.getKind() == ElementKind.INTERFACE) {
                return builder.add("return Optional.empty();").build();
            }

            Element[] constructors = target.getEnclosedElements().stream().filter(k -> k.getKind() == ElementKind.CONSTRUCTOR).toArray(Element[]::new);
            Element[] injectionConstructor = Arrays.stream(constructors)
                    .filter(k -> k.getAnnotationMirrors().stream().anyMatch(in -> in.getAnnotationType().toString().equals(Inject.class.getName())))
                    .toArray(Element[]::new);
            if (injectionConstructor.length == 1) {
                Element c = injectionConstructor[0];
                if (c instanceof ExecutableElement) {
                    ExecutableElement constructorParams = (ExecutableElement) c;
                    builder.add("$T result = new $T($L); \n",
                            targetClass,
                            targetClass,
                            CodeBlock.join(constructorParams.getParameters().stream().map(k ->
                                    buildResolution("resolveConstructorArgument", k, ClassName.get(target))
                            ).collect(Collectors.toList()), ","));
                }
            } else {
                builder.add("$T result = new $T();\n", targetClass, targetClass);
            }
            return builder.add("return this.inject(result,resolution);").build();
        }

        private CodeBlock buildResolution(String method, VariableElement element, ClassName target) {

            if (typeUtils.isAssignable(element.asType(), typeUtils.erasure(elementUtils.getTypeElement("java.util.Optional").asType()))) {
                return CodeBlock.builder().add("resolution.$L($L)",
                        method,
                        buildArgument(element)
                ).build();
            }

            return CodeBlock.builder().add("($T)requiredDependency(resolution.$L($L),() -> new $T($T.class, $T.class))",
                    TypeName.get(element.asType()),
                    method,
                    buildArgument(element),
                    ClassName.get("org.terasology.gestalt.di.exceptions", "DependencyResolutionException"),
                    target,
                    processingEnv.getTypeUtils().erasure(element.asType())
            ).build();
        }


        private CodeBlock buildInjectionBlock(TypeElement target) {
            String name = "instance";
            CodeBlock.Builder builder = CodeBlock.builder();

            TypeElement targetElement = target;
            while (true) {

                for (Element ele : targetElement.getEnclosedElements()) {
                    if (ele.getAnnotationMirrors().stream().anyMatch(in -> in.getAnnotationType().toString().equals(Inject.class.getName()))) {
                        if (ele instanceof ExecutableElement) {
                            List<? extends VariableElement> parameters = ((ExecutableElement) ele).getParameters();

                            if (ele.getSimpleName().toString().startsWith("set") && parameters.size() == 1) {
                                builder.add("$L.$L($L); \n", name, ele.getSimpleName(), buildResolution("resolveParameterArgument", parameters.get(0), ClassName.get(target)));
                            }
                        }
                        if (ele instanceof VariableElement) {
                            builder.add("$L.$L = $L; \n", name, ele.getSimpleName(), buildResolution("resolveParameterArgument", (VariableElement) ele, ClassName.get(target)));
                        }
                    }
                }

                TypeMirror superType = targetElement.getSuperclass();
                if(superType.getKind() == TypeKind.NONE) {
                    break;
                }
                targetElement = (TypeElement) typeUtils.asElement(superType);
            }
            return builder.add("return $L.$L($L);", ClassName.get(Optional.class), "of", name).build();
        }

        private CodeBlock buildArgumentBlock() {
            return CodeBlock.builder().add("new $T[]{$L}",
                    ClassName.get(BASE_PACKAGE, "Argument"),
                    CodeBlock.join(this.arguments, ",")
            ).build();
        }


        private void writeBeanDefinition(TypeElement typeElement, String className) {
            PackageElement packageElement = processingEnv.getElementUtils().getPackageOf(typeElement);

            TypeElement beanDefinitionClass = utility.getElements().getTypeElement("org.terasology.context.AbstractBeanDefinition");
            // TODO: flattened class tree can have collisions
            writer.writeService("org.terasology.context.BeanDefinition", packageElement.getQualifiedName() + "." + typeElement.getSimpleName() + "$BeanDefinition");

            CodeBlock injectionCreationBuilder = buildObjectPointCreationBlock(typeElement);
            CodeBlock injectionBlock = buildInjectionBlock(typeElement);
            CodeBlock classAnnotationBuilder = buildAnnotationMetadataBlock(typeElement);

            // argument block is built last
            CodeBlock argumentBlockBuilder = buildArgumentBlock();

            MethodSpec.Builder injectMethod = MethodSpec.overriding(beanDefinitionClass
                    .getEnclosedElements().stream()
                    .filter(elem -> elem instanceof ExecutableElement && elem.getSimpleName().contentEquals("inject"))
                    .map(elem -> (ExecutableElement) elem)
                    .findFirst()
                    .get()
            ).returns(TypeName.get(Optional.class)).addCode(injectionBlock);
            injectMethod.parameters.set(0, ParameterSpec.builder(TypeName.get(typeElement.asType()), "instance").build());

            JavaFile.Builder builder = JavaFile.builder(
                    packageElement.getQualifiedName().toString(),
                    TypeSpec.classBuilder(typeElement.getSimpleName().toString() + "$BeanDefinition")
                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                            .superclass(ParameterizedTypeName.get(
                                    ClassName.get(beanDefinitionClass),
                                    TypeName.get(typeElement.asType()))
                            )
                            .addField(FieldSpec.builder(ClassName.get(BASE_PACKAGE, "AnnotationMetadata"), CLASS_METADATA_FIELD, Modifier.FINAL, Modifier.STATIC, Modifier.PUBLIC)
                                    .initializer(classAnnotationBuilder).build())
                            .addField(FieldSpec.builder(ArrayTypeName.of(ClassName.get(BASE_PACKAGE, "Argument")), ARGUMENT_FIELD, Modifier.FINAL, Modifier.STATIC, Modifier.PUBLIC)
                                    .initializer(argumentBlockBuilder).build())
                            .addMethod(
                                    MethodSpec.overriding(beanDefinitionClass
                                            .getEnclosedElements().stream()
                                            .filter(elem -> elem instanceof ExecutableElement && elem.getSimpleName().contentEquals("targetClass"))
                                            .map(elem -> (ExecutableElement) elem)
                                            .findFirst()
                                            .get()
                                    ).returns(ParameterizedTypeName.get(ClassName.get(Class.class), TypeName.get(typeElement.asType())))
                                            .addCode("return $T.class;", typeElement).build()
                            )
                            .addMethod(
                                    MethodSpec.overriding(beanDefinitionClass
                                            .getEnclosedElements().stream()
                                            .filter(elem -> elem instanceof ExecutableElement && elem.getSimpleName().contentEquals("getAnnotationMetadata"))
                                            .map(elem -> (ExecutableElement) elem)
                                            .findFirst()
                                            .get()
                                    ).returns(ClassName.get(BASE_PACKAGE, "AnnotationMetadata"))
                                            .addCode("return $L;", CLASS_METADATA_FIELD).build()
                            )
                            .addMethod(
                                    MethodSpec.overriding(beanDefinitionClass
                                            .getEnclosedElements().stream()
                                            .filter(elem -> elem instanceof ExecutableElement && elem.getSimpleName().contentEquals("getArguments"))
                                            .map(elem -> (ExecutableElement) elem)
                                            .findFirst()
                                            .get()
                                    ).returns(ArrayTypeName.of(ClassName.get(BASE_PACKAGE, "Argument")))
                                            .addCode("return $L;", ARGUMENT_FIELD).build()
                            )
                            .addMethod(injectMethod.build())
                            .addMethod(
                                    MethodSpec.overriding(beanDefinitionClass
                                            .getEnclosedElements().stream()
                                            .filter(elem -> elem instanceof ExecutableElement && elem.getSimpleName().contentEquals("build"))
                                            .map(elem -> (ExecutableElement) elem)
                                            .findFirst()
                                            .get()
                                    ).returns(TypeName.get(Optional.class)).addCode("$L", injectionCreationBuilder).build()
                            )
                            .build());
            try {
                builder.build().writeTo(filer);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }
}
