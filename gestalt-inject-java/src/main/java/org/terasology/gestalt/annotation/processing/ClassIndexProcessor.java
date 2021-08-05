package org.terasology.gestalt.annotation.processing;

import com.google.common.collect.Queues;
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
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Queue;
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
            processAnnotationIndex(roundEnv, annotation);
            // Subtypes index. classes under index.
            processSubtypeIndexByDirectMarked(roundEnv, annotation);
        }

        // Subtypes index. every class, which can have interface with `@IndexInherited` annotation.
        for (Element type : roundEnv.getRootElements()) {
            processSubtypeIndexInReverseWay(type);
        }

        if (roundEnv.processingOver()) {
            writeIndexes();
        }
        return false;
    }

    private void processSubtypeIndexInReverseWay(Element type) {
        if (type.getKind() == ElementKind.CLASS) {
            Queue<TypeMirror> supers = Queues.newArrayDeque();
            for (Element subType : type.getEnclosedElements()) {
                processSubtypeIndexInReverseWay(subType);
            }
            supers.addAll(elementUtility.getTypes().directSupertypes(type.asType()));
            while (!supers.isEmpty()) {
                TypeMirror candidate = supers.poll();
                if (candidate.getKind() != TypeKind.NONE) {
                    if (elementUtility.hasStereotype(elementUtility.getTypes().asElement(candidate),
                            Collections.singletonList(IndexInherited.class.getName()))) {
                        TypeElement candidateElement = (TypeElement) elementUtility.getTypes().asElement(elementUtility.getTypes().erasure(candidate));
                        TypeElement erasedType = (TypeElement) elementUtility.getTypes().asElement(elementUtility.getTypes().erasure(type.asType()));
                        subtypesTypeWriter.writeSubType(elementUtility.getElements().getBinaryName(candidateElement).toString(),
                                elementUtility.getElements().getBinaryName(erasedType).toString());
                    }
                    supers.addAll(elementUtility.getTypes().directSupertypes(candidate));
                }
            }
        }
    }

    private void processSubtypeIndexByDirectMarked(RoundEnvironment roundEnv, TypeElement annotation) {
        if (Arrays.asList(Index.class.getName(), IndexInherited.class.getName()).contains(annotation.asType().toString())) {
            for (Element type : roundEnv.getElementsAnnotatedWith(annotation)) {
                if (type.getKind() == ElementKind.CLASS) {
                    TypeElement typeElement = (TypeElement) type;
                    Queue<TypeMirror> supers = Queues.newArrayDeque();
                    supers.addAll(elementUtility.getTypes().directSupertypes(typeElement.asType()));
                    while (!supers.isEmpty()) {
                        TypeMirror candidate = supers.poll();
                        if (candidate.getKind() != TypeKind.NONE) {
                            if (elementUtility.hasStereotype(elementUtility.getTypes().asElement(candidate),
                                    Collections.singletonList(IndexInherited.class.getName())))
                                subtypesTypeWriter.writeSubType(elementUtility.getTypes().erasure(candidate).toString(), elementUtility.getTypes().erasure(type.asType()).toString());
                            supers.addAll(elementUtility.getTypes().directSupertypes(candidate));
                        }
                    }
                }
            }
        }
    }

    private void processAnnotationIndex(RoundEnvironment roundEnv, TypeElement annotation) {
        if (elementUtility.hasStereotype(annotation, Collections.singletonList(Index.class.getName()))) {
            for (Element type : roundEnv.getElementsAnnotatedWith(annotation)) {
                if (type.getKind() == ElementKind.CLASS) {
                    annotationTypeWriter.writeAnnotation(annotation.getQualifiedName().toString(), elementUtility.getTypes().erasure(type.asType()).toString());
                } else if (type.getKind() == ElementKind.PACKAGE) {
                    PackageElement packageType = (PackageElement) type;
                    annotationTypeWriter.writeAnnotation(annotation.getQualifiedName().toString(), packageType.getQualifiedName().toString() + ".package-info");
                }
            }
        }
    }

    private void writeIndexes() {
        try {
            annotationTypeWriter.finish();
            subtypesTypeWriter.finish();
            FileObject file = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/gestalt-indexes-present");
            Writer writer = file.openWriter();
            writer.write("true");
            writer.close();
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Cannot write indexes: " + e.getMessage());
        }
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton("*");
    }
}
