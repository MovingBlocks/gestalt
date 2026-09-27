package org.terasology.gestalt.annotation.processing;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Queues;
import org.terasology.context.annotation.BindAnnotationFor;
import org.terasology.context.annotation.Index;
import org.terasology.context.annotation.IndexInherited;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationValue;
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
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

/**
 * Records all types annotated with `@Index` or `@IndexInherited`, as well as all descendants of a type annotated with `@IndexInherited`.
 */
public class ClassIndexProcessor extends AbstractProcessor {

    private Filer filer;
    private AnnotationTypeWriter annotationTypeWriter;
    private SubtypesTypeWriter subtypesTypeWriter;
    private ElementUtility elementUtility;

    private Multimap<TypeMirror, Class<? extends Annotation>> boundAnnotations;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        filer = processingEnv.getFiler();
        annotationTypeWriter = new AnnotationTypeWriter(filer);
        subtypesTypeWriter = new SubtypesTypeWriter(filer);
        elementUtility = new ElementUtility(processingEnv.getElementUtils(), processingEnv.getTypeUtils());
        boundAnnotations = HashMultimap.create();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            if (annotation.asType().toString().equals(BindAnnotationFor.class.getName())) {
                for (Element type : roundEnv.getElementsAnnotatedWith(annotation)) {
                    TypeMirror foreighElement = getBindAnnotationFor(type);
                    if (elementUtility.hasStereotype(type, Collections.singletonList(IndexInherited.class.getName()))) {
                        boundAnnotations.put(foreighElement, IndexInherited.class);
                    }
                    if (elementUtility.hasStereotype(type, Collections.singletonList(Index.class.getName()))) {
                        boundAnnotations.put(foreighElement, Index.class);
                    }
                }
            }
        }

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
                                Collections.singletonList(IndexInherited.class.getName())) || boundAnnotations.containsEntry(candidate, IndexInherited.class)) {
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
                                    Collections.singletonList(IndexInherited.class.getName())) || boundAnnotations.containsEntry(candidate, IndexInherited.class)) {
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
        }
    }

    private void processAnnotationIndex(RoundEnvironment roundEnv, TypeElement annotation) {
        if (elementUtility.hasStereotype(annotation, Collections.singletonList(Index.class.getName()))) {
            for (Element type : roundEnv.getElementsAnnotatedWith(annotation)) {
                if (type.getKind() == ElementKind.CLASS || type.getKind() == ElementKind.INTERFACE) {
                    TypeElement erasedType = (TypeElement) elementUtility.getTypes().asElement(elementUtility.getTypes().erasure(type.asType()));
                    annotationTypeWriter.writeAnnotation(annotation.getQualifiedName().toString(), elementUtility.getElements().getBinaryName(erasedType).toString());
                } else if (type.getKind() == ElementKind.PACKAGE) {
                    PackageElement packageType = (PackageElement) type;
                    annotationTypeWriter.writeAnnotation(annotation.getQualifiedName().toString(), packageType.getQualifiedName().toString() + ".package-info");
                }
            }
        }
    }

    private TypeMirror getBindAnnotationFor(Element element) {
        return (TypeMirror) getAnnotationValue(element, BindAnnotationFor.class, "value").orElse(null);
    }

    private Optional<Object> getAnnotationValue(Element element, Class<?> annotation, String fieldName) {
        return element
                .getAnnotationMirrors()
                .stream()
                .filter(am -> am.getAnnotationType().toString().equals(annotation.getName()))
                .map(am -> am.getElementValues()
                        .entrySet()
                        .stream()
                        .filter(kv -> kv.getKey().getSimpleName().toString().equals(fieldName))
                        .map(Map.Entry::getValue)
                        .findFirst())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(AnnotationValue::getValue)
                .findFirst();
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

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}
