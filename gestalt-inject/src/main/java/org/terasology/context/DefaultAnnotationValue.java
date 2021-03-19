// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.context;

import com.google.common.collect.Maps;
import org.terasology.context.annotation.UsedByGeneratedCode;

import javax.annotation.Nonnull;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;

/**
 * Generated annotation value usually filled by code generation.
 * @param <S>
 */
@UsedByGeneratedCode
public class DefaultAnnotationValue<S extends Annotation> implements AnnotationValue<S> {
    private final String annotationName;
    private final Map<String, Object> defaultValues;
    private final Map<String, Object> values;
    private final Class<S> annType;
    private final Map<String, AnnotationValue<Annotation>[]> annotations = Maps.newHashMap();

    public DefaultAnnotationValue(Class<S> annType, String name, Map<String, Object> values, Map<String, Object> defaultValues) {
        this(annType, name, values, defaultValues, new AnnotationValue[]{});
    }

    public DefaultAnnotationValue(Class<S> annType, String name, Map<String, Object> defaultValues, Map<String, Object> values, AnnotationValue[] annotations) {
        this.annotationName = name;
        this.defaultValues = defaultValues;
        this.values = values;
        this.annType = annType;
        Arrays.stream(annotations).collect(Collectors.groupingBy(k -> k.getAnnotationName())).forEach((k, v) -> {
            this.annotations.putIfAbsent(k, v.toArray(new AnnotationValue[0]));
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

    @Override
    public <T extends Annotation> AnnotationValue<T>[] getAnnotation(Class<T> annotation) {
        return getAnnotation(annotation.getName());
    }

    @Override
    public AnnotationValue[] getAnnotation(String annotation) {
        return annotations.get(annotation);
    }

    @Override
    public boolean hasStereotype(String annotation) {
        return annotations.containsKey(annotation);
    }

    @Override
    @Nonnull
    public Iterator<AnnotationValue<Annotation>[]> iterator() {
        return annotations.values().iterator();
    }

    @Override
    public List<AnnotationValue<Annotation>> getAnnotationsByStereotype(String stereotype) {
        List<AnnotationValue<Annotation>> result = new ArrayList<>(5);
        if (annotations.containsKey(stereotype)) {
            result.add((AnnotationValue<Annotation>) this);
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
    public List<AnnotationValue<Annotation>> getAnnotationsByStereotype(Class<? extends Annotation> stereotype) {
        return getAnnotationsByStereotype(stereotype.getName());
    }

    @Override
    public List<AnnotationValue<Annotation>> findAnnotations(String annotation) {
        List<AnnotationValue<Annotation>> results = new ArrayList<>(5);
        for (Map.Entry<String, AnnotationValue<Annotation>[]> pairs : annotations.entrySet()) {
            if (pairs.getKey().equals(annotation)) {
                results.addAll(Arrays.asList(pairs.getValue()));
            } else {
                for (AnnotationValue<Annotation> metadata : pairs.getValue()) {
                    results.addAll(metadata.findAnnotations(annotation));
                }
            }
        }
        return results;
    }

    @Override
    public  List<AnnotationValue<Annotation>> findAnnotations(Class<? extends Annotation> annotation) {
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
