// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.gestalt.di.exceptions;

import org.terasology.context.exception.DependencyInjectionException;
import org.terasology.gestalt.di.BeanContext;

public class UnknownContextTypeException extends DependencyInjectionException {
    public UnknownContextTypeException(BeanContext context) {
        super("Unknown context type: " + context.getClass());
    }

}
