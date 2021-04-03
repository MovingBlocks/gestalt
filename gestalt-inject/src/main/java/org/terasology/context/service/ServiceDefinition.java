// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.context.service;

import java.util.function.Supplier;

/**
 * A service that may or may not be present on the classpath.
 *
 * @param <T> The service type
 */
public interface ServiceDefinition<T> {

    /**
     * @return The full class name of the service
     */
    String getName();

    /**
     * @return is the service present
     */
    boolean isPresent();

    /**
     * Load the service of throw the given exception.
     *
     * @param exceptionSupplier The exception supplier
     * @param <X>               The exception type
     * @return The instance
     * @throws X The exception concrete type
     */
    <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X;

    /**
     * @return load the service
     */
    T load();
}
