package org.terasology.context;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DefaultAnnotationValue<S extends Annotation> implements AnnotationValue<S> {
    private String annotationName;
    private Map<String, Object> defaultValues;
    private Map<String, Object> values;
    private Map<String, DefaultAnnotationValue[]> annotations;

    public DefaultAnnotationValue(String name) {

    }

    @Override
    public String getAnnotationName() {
        return annotationName;
    }


    @Override
    public boolean hasStereotype(Class<? extends Annotation> annotation) {
        return hasStereotype(annotation.getName());
    }

    public <T extends Annotation> AnnotationValue<T>[] getAnnotation(Class<T> annotation) {
        return annotations.get(annotation.getName());
    }

    public boolean hasStereotype(String annotation) {
        if (annotations.containsKey(annotation)) {
            return true;
        }
        for (DefaultAnnotationValue[] annotations : annotations.values()) {
            for (DefaultAnnotationValue metadata : annotations) {
                if (metadata.hasStereotype(annotation)) {
                    return true;
                }
            }
        }
        return false;
    }


    @Override
    public List<AnnotationValue> getAnnotationsByStereotype(String stereotype) {
        List<AnnotationValue> result = new ArrayList<>(10);
        if (annotations.containsKey(stereotype)) {
            result.add(this);
        }
        for (DefaultAnnotationValue[] annotations : annotations.values()) {
            for (DefaultAnnotationValue metadata : annotations) {
                metadata.internalGetAnnotationByStereotype(stereotype, result);
            }
        }
        return result;
    }

    private void internalGetAnnotationByStereotype(String stereotype, List<AnnotationValue> result) {
        if (annotations.containsKey(stereotype)) {
            result.add(this);
        }
        for (DefaultAnnotationValue[] annotations : annotations.values()) {
            for (DefaultAnnotationValue metadata : annotations) {
                metadata.internalGetAnnotationByStereotype(stereotype, result);
            }
        }
    }


    @Override
    public boolean hasAnnotation(Class<? extends Annotation> annotation) {
        return hasAnnotation(annotation.getName());
    }

    @Override
    public boolean hasAnnotation(String annotation) {
        return annotations.containsKey(annotation);
    }

    @Override
    public Object getRawValue(String field) {
        Object result = values.get(field);
        if (result == null) {
            return defaultValues.get(field);
        }
        return result;
    }
}
