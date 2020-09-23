package org.terasology.gestalt.annotation.processing;

import com.squareup.javapoet.*;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementScanner8;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


@SupportedOptions({"org.terasology.gestalt.annotation.processing"})
public class BeanDefinitionProcessor extends AbstractProcessor {
    private static final String EVENT_INTERFACE = "org.terasology.gestalt.entitysystem.event.Event";

    private static final String RECEIVE_EVENT_TYPE = "org.terasology.gestalt.entitysystem.event.ReceiveEvent";

    private static final String[] TARGET_ANNOTATIONS = new String[]{
            "javax.inject.Inject",
            "javax.inject.Qualifier",
            "javax.inject.Singleton",
            RECEIVE_EVENT_TYPE
    };

    private Filer filer;
    private ServiceTypeWriter writer;
    private ElementUtility utility;
    private Messager messager;

    private Set<String> processed = new HashSet<>();
    private Set<String> beanDefinitions;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        this.filer = processingEnvironment.getFiler();
        this.writer = new ServiceTypeWriter(this.filer);
        this.messager = processingEnvironment.getMessager();

        this.utility = new ElementUtility(processingEnvironment.getElementUtils(), processingEnvironment.getTypeUtils());
        beanDefinitions = new HashSet<>();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton("*");
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment environment) {

        Set<? extends TypeElement> filteredAnnotation = annotations.stream()
                .filter(ann -> utility.hasStereotype(ann, Arrays.asList(TARGET_ANNOTATIONS)))
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
                    if (!beanDefinitions.contains(name) && !processed.contains(name)) {
                        if (typeElement.getKind() != ElementKind.INTERFACE) {
                            beanDefinitions.add(name);
                        }
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
                e.printStackTrace();
            }
        }
        return false;
    }

    public class DefinitionWriter extends ElementScanner8<Object, String> {
        private final TypeElement concreteClass;

        DefinitionWriter(TypeElement concreteClass) {
            this.concreteClass = concreteClass;
        }

        @Override
        public Object visitVariable(VariableElement e, String o) {
            return super.visitVariable(e, o);
        }

        @Override
        public Object visitPackage(PackageElement e, String o) {
            return super.visitPackage(e, o);
        }

        @Override
        public Object visitType(TypeElement e, String className) {
            writeBeanDefinition(e, className);
            return super.visitType(e, className);
        }

        private CodeBlock buildDefaultValues(Element declaredType) {
            for(Element target: declaredType.getEnclosedElements()){
                if(target.getKind() == ElementKind.METHOD){
                    ExecutableElement executableElement = (ExecutableElement) target;
                    TypeMirror returnType = executableElement.getReturnType();
                    Object defaultValue = executableElement.getDefaultValue().getValue();


                    returnType.toString();

                }

            }
            return null;
        }

        private Object getValue(Object target){
            Object result;
            if(target instanceof String){
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
                        defaults.add(CodeBlock.of("$S,$L", executableElement.getSimpleName(), getValue(value.getValue())));
                    }
                }

                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : ann.getElementValues().entrySet()) {
                    ExecutableElement executableElement = entry.getKey();
                    AnnotationValue value = entry.getValue();
                    values.add(CodeBlock.of("$S,$L", executableElement.getSimpleName(), getValue(value.getValue())));
                }

                List<? extends AnnotationMirror> children = declaredType.asElement().getAnnotationMirrors().stream().filter(k -> {
                    String name = k.getAnnotationType().toString();
                    return !name.startsWith("java.lang");
                }).collect(Collectors.toList());

                annotationsBuilder.add(CodeBlock.builder().add("new $T($S,$T.of($L),$T.of($L),new $T[] {$L})",
                    ClassName.get("org.terasology.context", "DefaultAnnotationValue"),
                    declaredType.toString(),
                    ClassName.get("org.terasology.context", "AnnotationMetaUtil"),
                    CodeBlock.join(defaults, ","),
                    ClassName.get("org.terasology.context", "AnnotationMetaUtil"),
                    CodeBlock.join(values, ","),
                    ClassName.get("org.terasology.context", "AnnotationValue"),
                    CodeBlock.join(buildAnnotationValues(children), ",")).build());
            }
            return annotationsBuilder;
        }

        private CodeBlock buildAnnotationMetadata(TypeElement element) {
            List<CodeBlock> blocks = buildAnnotationValues(element.getAnnotationMirrors());
            return CodeBlock.builder().add("new $T(new $T[]{$L})",
                ClassName.get("org.terasology.context", "DefaultAnnotationMetadata"),
                ClassName.get("org.terasology.context", "AnnotationValue"),
                CodeBlock.join(blocks,",")).build();

        }


        private void writeBeanDefinition(TypeElement typeElement, String className) {

            TypeElement beanDefinitionClass = utility.getElements().getTypeElement("org.terasology.context.AbstractBeanDefinition");
            writer.writeService("org.terasology.context.BeanDefinition", className + "$BeanDefinition");
            JavaFile javaFile = JavaFile.builder(
                className.substring(0, className.lastIndexOf('.')),
                TypeSpec.classBuilder(typeElement.getSimpleName().toString() + "$BeanDefinition")
                    .superclass(ParameterizedTypeName.get(
                        ClassName.get(beanDefinitionClass),
                        TypeName.get(typeElement.asType()))
                    )
                    .addMethod(
                        MethodSpec.overriding(beanDefinitionClass
                            .getEnclosedElements().stream()
                            .filter((elem) -> elem instanceof ExecutableElement && elem.getSimpleName().contentEquals("isSingleton"))
                            .map((elem) -> (ExecutableElement) elem)
                            .findFirst()
                            .get()
                        ).addCode("return true;").build()
                    )
                    .addMethod(
                        MethodSpec.overriding(beanDefinitionClass
                            .getEnclosedElements().stream()
                            .filter((elem) -> elem instanceof ExecutableElement && elem.getSimpleName().contentEquals("targetClass"))
                            .map((elem) -> (ExecutableElement) elem)
                            .findFirst()
                            .get()
                        ).returns(ParameterizedTypeName.get(ClassName.get(Class.class), TypeName.get(typeElement.asType())))
                            .addCode("return $T.class;", typeElement).build()
                    )
                    .addMethod(
                        MethodSpec.overriding(beanDefinitionClass
                            .getEnclosedElements().stream()
                            .filter((elem) -> elem instanceof ExecutableElement && elem.getSimpleName().contentEquals("getAnnotationMetadata"))
                            .map((elem) -> (ExecutableElement) elem)
                            .findFirst()
                            .get()
                        ).returns(ClassName.get("org.terasology.context", "AnnotationMetadata"))
                            .addCode("return $L;", buildAnnotationMetadata(typeElement)).build()
                    )
                    .build())
                .build();
            try {
                javaFile.writeTo(filer);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }

        @Override
        public Object visitExecutable(ExecutableElement e, String o) {
            return super.visitExecutable(e, o);
        }

        @Override
        public Object visitTypeParameter(TypeParameterElement e, String o) {
            return super.visitTypeParameter(e, o);
        }

        @Override
        public Object visitUnknown(Element e, String o) {
            return super.visitUnknown(e, o);
        }
    }
}
