package org.terasology.gestalt.di.exceptions;

import org.terasology.context.exception.DependencyInjectionException;
import org.terasology.gestalt.di.BeanContext;

public class UnknownContextTypeException extends DependencyInjectionException {
    public UnknownContextTypeException(BeanContext context) {
        super("Unknown context type: " + context.getClass());
    }
    
}
