package org.terasology.context;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

public class EmptyAnnotationMetadata implements AnnotationMetadata {
   public final static EmptyAnnotationMetadata EMPTY_ARGUMENT = new EmptyAnnotationMetadata();

    @Override
    public List<AnnotationValue> getAnnotationsByStereotype(Class<? extends Annotation> stereotype) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<AnnotationValue> getAnnotationsByStereotype(String stereotype) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public boolean hasAnnotation(Class<? extends Annotation> annotation) {
        return false;
    }

    @Override
    public boolean hasAnnotation(String annotation) {
        return false;
    }

    @Override
    public boolean hasStereotype(Class<? extends Annotation> annotation) {
        return false;
    }

    @Override
    public boolean hasStereotype(String annotation) {
        return false;
    }

    @Override
    public Object getRawSingleValue(String annotation, String field) {
        return null;
    }
}
