package org.terasology.gestalt.annotation.processing;


import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementScanner8;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


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
                if(!beanDefinitions.contains(name) && !processed.contains(name)){
                    if(typeElement.getKind() != ElementKind.INTERFACE){
                        beanDefinitions.add(name);
                    }
                }
            }));

        for(String name: processed){
            beanDefinitions.remove(name);
        }

        int count = beanDefinitions.size();
        if(count > 0) {
            this.messager.printMessage(Diagnostic.Kind.NOTE,String.format("Creating bean classes for %s type elements", count));
            beanDefinitions.forEach(className -> {
                if(processed.add(className)) {
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

    public static class DefinitionWriter extends ElementScanner8<Object, Object> {
        private final TypeElement concreteClass;

        DefinitionWriter(TypeElement concreteClass) {
            this.concreteClass = concreteClass;
        }
    }
}
