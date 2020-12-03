package org.terasology.context;

import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.List;

public interface AnnotationValue<S extends Annotation> extends Iterable<AnnotationValue[]> {

    String getAnnotationName();

    Class<S> getAnnotationType();

    boolean hasAnnotation(String annotation);

    boolean hasAnnotation(Class<? extends Annotation> annotation);

    Object getRawValue(String field);

    boolean hasStereotype(Class<? extends Annotation> ann);

    boolean hasStereotype(String ann);

    List<AnnotationValue> getAnnotationsByStereotype(String stereotype);
}
