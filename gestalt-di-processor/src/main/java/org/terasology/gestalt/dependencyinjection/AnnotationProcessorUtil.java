package org.terasology.gestalt.dependencyinjection;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

/**
 * Utility methods to aid annotation processing
 */
public final class AnnotationProcessorUtil {
    private AnnotationProcessorUtil() {
    }

    /**
     * Obtains a specific annotation from an ExecutableElement (method, typically)
     * @param executableElement The executableElement to obtain the annotation from
     * @param annotationClass The type of annotation to obtain
     * @return An AnnotationMirror describing the annotation, or null if the annotation is not present
     */
    public static AnnotationMirror getAnnotation(ExecutableElement executableElement, Class<?> annotationClass) {
        String typeName = annotationClass.getName();
        for (AnnotationMirror mirror : executableElement.getAnnotationMirrors()) {
            if (mirror.getAnnotationType().toString().equals(typeName)) {
                return mirror;
            }
        }
        return null;
    }

    /**
     * Obtains a specific annotation from an ExecutableElement (method, typically)
     * @param typeElement The typeElement to obtain the annotation from
     * @param annotationClass The type of annotation to obtain
     * @return An AnnotationMirror describing the annotation, or null if the annotation is not present
     */
    public static AnnotationMirror getAnnotation(TypeElement typeElement, Class<?> annotationClass) {
        String typeName = annotationClass.getName();
        for (AnnotationMirror mirror : typeElement.getAnnotationMirrors()) {
            if (mirror.getAnnotationType().toString().equals(typeName)) {
                return mirror;
            }
        }
        return null;
    }

    /**
     * Obtain a value from an Annotation mirror
     * @param annotation The annotation mirror
     * @param key The key of the annotation attribute
     * @return The AnnotationValue, or null if the attribute is not set
     */
    public static AnnotationValue getAnnotationValue(AnnotationMirror annotation, String key) {
        return annotation.getElementValues().entrySet().stream().filter(x -> x.getKey().getSimpleName().toString().equals(key)).map(Map.Entry::getValue).findAny().orElse(null);
    }

    /**
     * Obtain a class list from an attribute on an annotation on a type
     * @param typeElement The type
     * @param annotationClass The annotation type
     * @param attribute The name of the attribute of a class list
     * @return A list of type mirrors - will be empty if the annotation is not present or attribute not set
     */
    public static List<TypeMirror> getAnnotationClassList(TypeElement typeElement, Class<?> annotationClass, String attribute) {
        AnnotationMirror annotation = getAnnotation(typeElement, annotationClass);
        if (annotation != null) {
            AnnotationValue annotationValue = getAnnotationValue(annotation, attribute);
            return ((List<AnnotationValue>) annotationValue.getValue()).stream().map(x -> (TypeMirror) x.getValue()).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

}
