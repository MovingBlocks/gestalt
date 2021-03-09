// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.context;

public class DefaultArgument<T> implements Argument<T> {

    private final Class<T> type;
    private final AnnotationMetadata annotationMetadata;

    public DefaultArgument(Class<T> type, AnnotationMetadata annotationMetadata) {
        this.type = type;
        this.annotationMetadata = annotationMetadata;
    }

    @Override
    public String getName() {
        return type.getSimpleName();
    }

    @Override
    public Class<T> getType() {
        return type;
    }

    @Override
    public AnnotationMetadata getAnnotation() {
        return this.annotationMetadata;
    }

}
