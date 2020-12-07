package org.terasology.context;

import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public interface AnnotationValue<S extends Annotation> extends Iterable<AnnotationValue[]> {

    String getAnnotationName();

    Class<S> getAnnotationType();

    boolean hasAnnotation(String annotation);

    boolean hasAnnotation(Class<? extends Annotation> annotation);

    <T extends Annotation> AnnotationValue<T>[] getAnnotation(Class<T> annotation);

    AnnotationValue[] getAnnotation(String annotation);

    boolean hasStereotype(Class<? extends Annotation> ann);

    boolean hasStereotype(String ann);

    List<AnnotationValue> getAnnotationsByStereotype(String stereotype);

    List<AnnotationValue> getAnnotationsByStereotype(Class<? extends Annotation> stereotype);

    List<AnnotationValue> findAnnotations(String annotation);

    <T extends Annotation> List<AnnotationValue> findAnnotations(Class<T> annotation);

    OptionalInt intValue(String field);

    Optional<String> stringValue(String field);

    boolean hasField(String field);

    Object getRawValue(String field);

}
