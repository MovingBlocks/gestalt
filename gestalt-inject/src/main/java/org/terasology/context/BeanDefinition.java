// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.context;

import org.terasology.context.exception.DependencyInjectionException;

import java.util.Optional;
import java.util.function.Supplier;

public interface BeanDefinition<T> {
    AnnotationMetadata getAnnotationMetadata();

    default <K> K requiredDependency(Optional<K> wrapper, Supplier<DependencyInjectionException> exceptionSupplier) {
        if(!wrapper.isPresent()) {
            throw exceptionSupplier.get();
        }
        return wrapper.get();
    }

    Optional<T> build(BeanResolution resolution);

    Optional<T> inject(T instance, BeanResolution resolution);

    Argument[] getArguments();

    Class[] getTypeArgument();

    Class<T> targetClass();

}
