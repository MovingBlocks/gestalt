// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.gestalt.di.exceptions;

import org.terasology.context.exception.DependencyInjectionException;

public class DependencyResolutionException extends DependencyInjectionException {
    public DependencyResolutionException(Class<?> provider, Class<?> target) {
        super("failed to inject dependency " + target.toString() + " into " + provider.toString());
    }
}
