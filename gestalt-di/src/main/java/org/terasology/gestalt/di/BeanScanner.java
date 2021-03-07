// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.gestalt.di;

public interface BeanScanner {
    void apply(ServiceRegistry registry, BeanEnvironment environment);
}
