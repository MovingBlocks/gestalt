package org.terasology.context;

import java.lang.annotation.Annotation;
import java.util.List;

public interface AnnotationMetadata {

    List<AnnotationValue<Annotation>> getAnnotationsByStereotype(Class<? extends Annotation> stereotype);

    List<AnnotationValue<Annotation>> getAnnotationsByStereotype(String stereotype);

    List<AnnotationValue<Annotation>> findAnnotations(String annotation);

    List<AnnotationValue<Annotation>>  findAnnotations(Class<? extends Annotation> annotation);

    boolean hasAnnotation(Class<? extends Annotation> annotation);

    boolean hasAnnotation(String annotation);

    boolean hasStereotype(Class<? extends Annotation> annotation);

    boolean hasStereotype(String annotation);

    Object getRawSingleValue(String annotation, String field);
}
