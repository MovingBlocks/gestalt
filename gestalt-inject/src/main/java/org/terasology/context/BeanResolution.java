// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.context;

import org.terasology.context.exception.DependencyInjectionException;

import java.util.Optional;

public interface BeanResolution {

    <T> Optional<T> resolveConstructorArgument(Class<T> target, Argument<T> argument) throws DependencyInjectionException;

    <T> Optional<T> resolveParameterArgument(Class<T> target, Argument<T> argument) throws DependencyInjectionException;
}
