package org.terasology.context;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DefaultAnnotationValue<S extends Annotation> implements AnnotationValue<S> {
    private String annotationName;
    private Map<String, Object> defaultValues;
    private Map<String, Object> values;
    private Map<String, AnnotationValue[]> annotations;

    public DefaultAnnotationValue(String name, Map<String, Object> values, Map<String, Object> defaultValues, AnnotationValue[] annotations) {
        this.annotationName = name;
        this.defaultValues = defaultValues;
        this.values = values;
        Arrays.stream(annotations).collect(Collectors.groupingBy(k -> k.getAnnotationName())).forEach((k, v) -> {
            this.annotations.putIfAbsent(k, v.toArray(new DefaultAnnotationValue[0]));
        });
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
        for (AnnotationValue[] annotations : annotations.values()) {
            for (AnnotationValue metadata : annotations) {
                if (metadata.hasStereotype(annotation)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Iterator<AnnotationValue[]> iterator() {
        return annotations.values().iterator();
    }

    @Override
    public List<AnnotationValue> getAnnotationsByStereotype(String stereotype) {
        List<AnnotationValue> result = new ArrayList<>(10);
        if (annotations.containsKey(stereotype)) {
            result.add(this);
        }
        for (AnnotationValue[] annotations : annotations.values()) {
            for (AnnotationValue metadata : annotations) {
                internalGetAnnotationByStereotype(metadata, stereotype, result);
            }
        }
        return result;
    }

    private void internalGetAnnotationByStereotype(AnnotationValue<? extends Annotation> target, String stereotype, List<AnnotationValue> result) {
        if (target.hasAnnotation(stereotype)) {
            result.add(this);
        }
        for (AnnotationValue[] annotations : target) {
            for (AnnotationValue metadata : annotations) {
                internalGetAnnotationByStereotype(metadata, stereotype, result);
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
