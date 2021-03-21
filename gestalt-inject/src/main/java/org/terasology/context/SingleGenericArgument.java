// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.context;

public class SingleGenericArgument<T,Y> implements Argument<T> {

    private final Class<T> type;
    private final Class<Y> genericType;
    private final AnnotationMetadata annotationMetadata;

    public SingleGenericArgument(Class<Y> genericType,Class<T> type, AnnotationMetadata annotationMetadata) {
        this.type = type;
        this.annotationMetadata = annotationMetadata;
        this.genericType = genericType;
    }

    public Class<Y> getGenericType() {
        return this.genericType;
    }

    @Override
    public Class<T> getType() {
        return type;
    }

    @Override
    public AnnotationMetadata getAnnotation() {
        return annotationMetadata;
    }

    @Override
    public String getName() {
        return type.getSimpleName();
    }
}
