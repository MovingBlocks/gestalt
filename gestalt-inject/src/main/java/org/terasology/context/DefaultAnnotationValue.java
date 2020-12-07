// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.context;

import com.google.common.collect.Maps;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;

public class DefaultAnnotationValue<S extends Annotation> implements AnnotationValue<S> {
    private String annotationName;
    private Map<String, Object> defaultValues;
    private Map<String, Object> values;
    private Class<S> annType;
    private Map<String, AnnotationValue[]> annotations = Maps.newHashMap();

    public DefaultAnnotationValue(Class<S> annType, String name, Map<String, Object> values, Map<String, Object> defaultValues) {
        this(annType, name, values, defaultValues, new AnnotationValue[]{});
    }

    public DefaultAnnotationValue(Class<S> annType, String name, Map<String, Object> values, Map<String, Object> defaultValues, AnnotationValue[] annotations) {
        this.annotationName = name;
        this.defaultValues = defaultValues;
        this.values = values;
        this.annType = annType;
        Arrays.stream(annotations).collect(Collectors.groupingBy(k -> k.getAnnotationName())).forEach((k, v) -> {
            this.annotations.putIfAbsent(k, v.toArray(new DefaultAnnotationValue[0]));
        });
    }

    @Override
    public String getAnnotationName() {
        return annotationName;
    }

    @Override
    public Class<S> getAnnotationType() {
        return this.annType;
    }


    @Override
    public boolean hasStereotype(Class<? extends Annotation> annotation) {
        return hasStereotype(annotation.getName());
    }

    public <T extends Annotation> AnnotationValue<T>[] getAnnotation(Class<T> annotation) {
        return getAnnotation(annotation.getName());
    }

    @Override
    public AnnotationValue[] getAnnotation(String annotation) {
        return annotations.get(annotation);
    }

    public boolean hasStereotype(String annotation) {
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
        List<AnnotationValue> result = new ArrayList<>(5);
        if (annotations.containsKey(stereotype)) {
            result.add(this);
            return result;
        }
        for (AnnotationValue[] annotations : annotations.values()) {
            for (AnnotationValue metadata : annotations) {
                result.addAll(metadata.getAnnotationsByStereotype(stereotype));
            }
        }
        return result;
    }

    @Override
    public List<AnnotationValue> getAnnotationsByStereotype(Class<? extends Annotation> stereotype) {
        return getAnnotationsByStereotype(stereotype.getName());
    }

    @Override
    public List<AnnotationValue> findAnnotations(String annotation) {
        List<AnnotationValue> results = new ArrayList<>(5);
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
    public <T extends Annotation> List<AnnotationValue> findAnnotations(Class<T> annotation) {
        return findAnnotations(annotation.getName());
    }

    @Override
    public OptionalInt intValue(String field) {
        Object o = getRawValue(field);
        if (o instanceof Number) {
            return OptionalInt.of(((Number) o).intValue());
        }
        return OptionalInt.empty();
    }

    @Override
    public Optional<String> stringValue(String field) {
        Object o = getRawValue(field);
        if (o instanceof String) {
            return Optional.of((String) o);
        }
        return Optional.empty();
    }

    @Override
    public boolean hasField(String field) {
        return defaultValues.containsKey(field) || values.containsKey(field);
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
