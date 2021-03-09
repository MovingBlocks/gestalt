// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.gestalt.di.exceptions;

import org.terasology.context.exception.DependencyInjectionException;
import org.terasology.gestalt.di.BeanKey;
import org.terasology.gestalt.di.instance.BeanProvider;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class BeanResolutionException extends DependencyInjectionException {
    public BeanResolutionException( BeanKey provider) {
        super("Failed to Resolve Bean" + provider.toString());
    }
    public BeanResolutionException(Iterable<BeanKey> providers) {
        super("Resolved Multiple Beans: " + StreamSupport.stream(providers.spliterator(), false).map(Object::toString).collect(Collectors.joining(",")));
    }

    public BeanResolutionException(Class clazz) {
        super("Failed to Resolve Bean" + clazz.toString());
    }

}
