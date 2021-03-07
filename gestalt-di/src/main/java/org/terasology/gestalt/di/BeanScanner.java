// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.gestalt.di;

/**
 * A scanner interface that is used against {@link BeanEnvironment} to resolve definitions
 */
public interface BeanScanner {
    void apply(ServiceRegistry registry, BeanEnvironment environment);
}
