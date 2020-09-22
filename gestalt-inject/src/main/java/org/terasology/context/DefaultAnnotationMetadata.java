package org.terasology.context;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public List<AnnotationValue> getAnnotationsByStereotype(String stereotype) {
        List<AnnotationValue> results = new ArrayList<>();
        if (annotations.containsKey(stereotype)) {
            results.addAll(Arrays.asList(annotations.get(stereotype)));
        }
        for (AnnotationValue[] annotations : annotations.values()) {
            for (AnnotationValue annotation : annotations) {
                results.addAll(annotation.getAnnotationsByStereotype(stereotype));
            }
        }
        return results;
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
