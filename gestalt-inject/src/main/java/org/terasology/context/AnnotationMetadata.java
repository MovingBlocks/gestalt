// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.context;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Annotation data that is used to drive avoid reflection.
 */
public interface AnnotationMetadata extends Iterable<AnnotationValue<Annotation>[]> {

    <T extends Annotation> List<AnnotationValue<Annotation>> getAnnotationsByStereotype(Class<T> stereotype);

    List<AnnotationValue<Annotation>> getAnnotationsByStereotype(String stereotype);

    <T extends Annotation> List<AnnotationValue<T>> findAnnotations(String annotation);

    <T extends Annotation> List<AnnotationValue<T>> findAnnotations(Class<T> annotation);

    boolean hasAnnotation(Class<? extends Annotation> annotation);

    boolean hasAnnotation(String annotation);

    boolean hasStereotype(Class<? extends Annotation> annotation);

    boolean hasStereotype(String annotation);

    Object getRawSingleValue(String annotation, String field);
}
