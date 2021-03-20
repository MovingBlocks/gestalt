// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.context;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Single annotation value with properties.
 * @param <S>
 */
public interface AnnotationValue<S extends Annotation> extends Iterable<AnnotationValue<Annotation>[]> {

    static Map<String,Object> of(Object... values){
        if(values.length == 0){
            return Collections.emptyMap();
        }
        HashMap<String,Object> result = new HashMap<>();
        int i = 0;
        while(i < values.length - 1){
            String key = values[i++].toString();
            Object value = values[i++];
            result.put(key,value);
        }
        return result;
    }

    /**
     * the name of the annotation {@link Class#getName()}
     * @return
     */
    String getAnnotationName();

    /**
     * annotation {@link Class} type
     * @return
     */
    Class<S> getAnnotationType();

    boolean hasAnnotation(String annotation);

    boolean hasAnnotation(Class<? extends Annotation> annotation);

    <T extends Annotation> AnnotationValue<T>[] getAnnotation(Class<T> annotation);

    AnnotationValue<Annotation>[] getAnnotation(String annotation);

    boolean hasStereotype(Class<? extends Annotation> ann);

    /**
     * test is representing annotation contains child stereotype
     * @param ann
     * @return
     */
    boolean hasStereotype(String ann);

    /**
     * find annotation that is is child to the stereotype ({@link Class#getName()})
     * @param stereotype the annotation
     * @return
     */
    List<AnnotationValue<Annotation>> getAnnotationsByStereotype(String stereotype);

    List<AnnotationValue<Annotation>> getAnnotationsByStereotype(Class<? extends Annotation> stereotype);

    /**
     * find annotations by base name({@link Class#getName()})
     * @param annotation name of annotation
     * @return annotation value representing annotation by name
     */
    List<AnnotationValue<Annotation>> findAnnotations(String annotation);

    /**
     * find annotations by {@link Class}
     * @param annotation target annotation
     * @return annotation extended by {@link Class}
     */
    List<AnnotationValue<Annotation>> findAnnotations(Class<? extends Annotation> annotation);

    /**
     * int from value defined in annotation
     * @param field field to lookup from annotation
     * @return an {@link Optional} value if existing for annotation
     */
    OptionalInt intValue(String field);

    /**
     * string from value defined in annotation. If field is not string then {@link Optional#empty()}
     * @param field field to lookup from annotation
     * @return an {@link Optional} value if existing for annotation
     */
    Optional<String> stringValue(String field);

    /**
     * check if field is defined in annotation
     * @param field field to lookup from annotation
     * @return does field exist
     */
    boolean hasField(String field);

    /**
     * raw value by field. null if it doesn't exist or  default value if it is defined for the annotation
     * @param field field to lookup from annotation
     * @return the raw field
     */
    Object getRawValue(String field);

}
