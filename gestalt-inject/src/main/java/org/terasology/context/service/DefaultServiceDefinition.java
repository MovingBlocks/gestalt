// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.context.service;


import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.function.Supplier;

/**
 * Default implementation of {@link ServiceDefinition}.
 *
 * @param <S> The type
 * @author Graeme Rocher
 * @since 1.0
 */
class DefaultServiceDefinition<S> implements ServiceDefinition<S> {
    private final String name;
    private final Optional<Class<S>> loadedClass;

    /**
     * @param name        The name
     * @param loadedClass The loaded class
     */
    DefaultServiceDefinition(String name, Optional<Class<S>> loadedClass) {
        this.name = name;
        this.loadedClass = loadedClass;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isPresent() {
        return loadedClass.isPresent();
    }

    @Override
    public <X extends Throwable> S orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        final Class<S> type = loadedClass.orElseThrow(exceptionSupplier);
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (Throwable e) {
            throw exceptionSupplier.get();
        }
    }

    @Override
    public S load() {
        return loadedClass.map(aClass -> {
            try {
                return aClass.getDeclaredConstructor().newInstance();
            } catch (Throwable e) {
                throw new ServiceConfigurationError("Error loading service [" + aClass.getName() + "]: " + e.getMessage(), e);
            }
        }).orElseThrow(() -> new ServiceConfigurationError("Call to load() when class '" + name + "' is not present"));
    }
}
