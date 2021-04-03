// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.context.exception;

public class BeanNotFoundException  extends DependencyInjectionException{
    public BeanNotFoundException() {
        super();
    }

    public BeanNotFoundException(String message) {
        super(message);
    }

    public BeanNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public BeanNotFoundException(Throwable cause) {
        super(cause);
    }

    protected BeanNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
