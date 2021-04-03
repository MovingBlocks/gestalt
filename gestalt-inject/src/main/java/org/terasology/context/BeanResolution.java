// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.context;

import org.terasology.context.exception.DependencyInjectionException;

import java.util.Optional;

/**
 * Bean Resolution for building an object
 */
public interface BeanResolution {

    /**
     * resolving an objects for injecting into constructor
     * @param argument the argument
     * @param <T> the type
     * @return the result
     * @throws DependencyInjectionException
     */
    <T> Optional<T> resolveConstructorArgument(Argument<T> argument) throws DependencyInjectionException;


    /**
     * resolving an objects for injecting into a field
     * @param argument the argument
     * @param <T> the type
     * @return the result
     * @throws DependencyInjectionException
     */
    <T> Optional<T> resolveParameterArgument(Argument<T> argument) throws DependencyInjectionException;
}
