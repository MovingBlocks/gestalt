package org.terasology.gestalt.annotation.processing;

import org.terasology.context.annotation.Index;
import org.terasology.context.annotation.IndexInherited;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

public class ClassIndexProcessor extends AbstractProcessor {


    private Filer filer;
    private AnnotationTypeWriter annotationTypeWriter;
    private SubtypesTypeWriter subtypesTypeWriter;
    private ElementUtility elementUtility;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        filer = processingEnv.getFiler();
        annotationTypeWriter = new AnnotationTypeWriter(filer);
        subtypesTypeWriter = new SubtypesTypeWriter(filer);
        elementUtility = new ElementUtility(processingEnv.getElementUtils(), processingEnv.getTypeUtils());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            // Annotation Index
            if (elementUtility.hasStereotype(annotation, Collections.singletonList(Index.class.getName()))) {
                for (Element type : roundEnv.getElementsAnnotatedWith(annotation)) {
                    if (type.getKind() == ElementKind.CLASS) {
                        annotationTypeWriter.writeAnnotation(annotation.getQualifiedName().toString(), type.asType().toString());
                    } else if (type.getKind() == ElementKind.PACKAGE) {
                        PackageElement packageType = (PackageElement) type;
                        for (Element enclosedElement : packageType.getEnclosedElements()) {
                            annotationTypeWriter.writeAnnotation(annotation.getQualifiedName().toString(), enclosedElement.asType().toString());
                        }
                    }
                }
            }
            if (Arrays.asList(Index.class.getName(), IndexInherited.class.getName()).contains(annotation)) {
                for (Element type : roundEnv.getElementsAnnotatedWith(annotation)) {
                    if (type.getKind() == ElementKind.CLASS) {
                        for (TypeMirror tm : ((TypeElement) type).getInterfaces()) {
                            subtypesTypeWriter.writeSubType(tm.toString(), type.asType().toString());
                        }
                    }
                }
            }
        }
        if (roundEnv.processingOver()) {
            try {
                annotationTypeWriter.finish();
                subtypesTypeWriter.finish();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton("*");
    }
}
