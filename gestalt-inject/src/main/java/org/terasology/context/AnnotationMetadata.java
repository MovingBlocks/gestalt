package org.terasology.context;

import java.lang.annotation.Annotation;

public interface AnnotationMetadata {

    <T> T getField(Class<? extends Annotation> ann, String field, Class<T> targetType);


    boolean hasAnnotation(Class<? extends Annotation> ann);

    boolean hasAnnotation(String ann);

    boolean hasStereotype(Class<? extends Annotation> ann);

    boolean hasStereotype(String ann);
}
