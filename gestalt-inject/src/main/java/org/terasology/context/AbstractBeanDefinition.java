// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.context;

import java.util.Optional;

public abstract class AbstractBeanDefinition<T> implements BeanDefinition<T> {

    public AnnotationMetadata getAnnotationMetadata() {
        return new DefaultAnnotationMetadata(new AnnotationValue[]{});
    }

    public Optional<T> build(BeanResolution resolution){
        return Optional.empty();
    }

    public Optional<T> inject(T instance, BeanResolution resolution){
        return Optional.of(instance);
    }

    public abstract Argument[] getArguments();

    @Override
    public Class[] getTypeArgument() {
        Class[] results = new Class[getArguments().length];
        Argument[] args = getArguments();
        for (int i = 0; i < args.length; i++) {
            results[i] = args[i].getType();
        }
        return results;
    }

    public abstract Class<T> targetClass();
}
