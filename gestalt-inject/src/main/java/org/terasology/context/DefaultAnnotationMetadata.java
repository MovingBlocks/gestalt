package org.terasology.context;

import java.lang.annotation.Annotation;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

public class DefaultAnnotationMetadata implements AnnotationMetadata {
    private final Map<String, AnnotationValue[]> annotations = new HashMap<>();

    public DefaultAnnotationMetadata(AnnotationValue[] annotations) {
        Arrays.stream(annotations).collect(Collectors.groupingBy(k -> k.getAnnotationName())).forEach((k, v) -> {
            this.annotations.putIfAbsent(k, v.toArray(v.toArray(new AnnotationValue[0])));
        });
    }

    public static DefaultAnnotationMetadata Build(AnnotationValue[] annotation) {
        return new DefaultAnnotationMetadata(annotation);
    }

    @Override
    public Object getRawSingleValue(String annotation, String field) {
        AnnotationValue[] result = annotations.get(annotation);
        if (result != null) {
            if (result.length == 1) {
                return result[0].getRawValue(field);
            }
        }
        return null;
    }

    @Override
    public List<AnnotationValue> getAnnotationsByStereotype(Class<? extends Annotation> stereotype) {
        return getAnnotationsByStereotype(stereotype.getName());
    }

    @Override
    public List<AnnotationValue> getAnnotationsByStereotype(String stereotype) {
        List<AnnotationValue> results = new ArrayList<>();
        for(Map.Entry<String, AnnotationValue[]> pairs : annotations.entrySet()) {
            if(pairs.getKey().equals(stereotype)) {
                continue;
            }
            for (AnnotationValue ann : pairs.getValue()) {
                results.addAll(ann.getAnnotationsByStereotype(stereotype));
            }
        }
        return results;
    }

    @Override
    public List<AnnotationValue> findAnnotations(String annotation) {
        List<AnnotationValue> results = new ArrayList<>();
        for (Map.Entry<String, AnnotationValue[]> pairs : annotations.entrySet()) {
            if (pairs.getKey().equals(annotation)) {
                results.addAll(Arrays.asList(pairs.getValue()));
            } else {
                for (AnnotationValue metadata : pairs.getValue()) {
                    results.addAll(metadata.findAnnotations(annotation));
                }
            }
        }
        return results;
    }

    @Override
    public List<AnnotationValue> findAnnotations(Class<? extends Annotation> annotation) {
        return findAnnotations(annotation.getName());
    }

    public boolean hasAnnotation(Class<? extends Annotation> annotation) {
        return annotations.containsKey(annotation.getName());
    }

    @Override
    public boolean hasAnnotation(String annotation) {
        return annotations.containsKey(annotation);
    }

    @Override
    public boolean hasStereotype(Class<? extends Annotation> ann) {
        return hasStereotype(ann.getName());
    }

    @Override
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
}
