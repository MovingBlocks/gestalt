// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.context;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public interface AnnotationValue<S extends Annotation> extends Iterable<AnnotationValue<Annotation>[]> {

    String getAnnotationName();

    Class<S> getAnnotationType();

    boolean hasAnnotation(String annotation);

    boolean hasAnnotation(Class<? extends Annotation> annotation);

    <T extends Annotation> AnnotationValue<T>[] getAnnotation(Class<T> annotation);

    AnnotationValue<Annotation>[] getAnnotation(String annotation);

    boolean hasStereotype(Class<? extends Annotation> ann);

    boolean hasStereotype(String ann);

    List<AnnotationValue<Annotation>> getAnnotationsByStereotype(String stereotype);

    List<AnnotationValue<Annotation>> getAnnotationsByStereotype(Class<? extends Annotation> stereotype);

    List<AnnotationValue<Annotation>> findAnnotations(String annotation);

    List<AnnotationValue<Annotation>> findAnnotations(Class<? extends Annotation> annotation);

    OptionalInt intValue(String field);

    Optional<String> stringValue(String field);

    boolean hasField(String field);

    Object getRawValue(String field);

}
