package org.terasology.gestalt.di.exceptions;

import org.terasology.context.exception.DependencyInjectionException;
import org.terasology.gestalt.di.BeanKey;

public class DependencyResolutionException extends DependencyInjectionException {
    public DependencyResolutionException(Class<?> provider, Class<?> target) {
        super("failed to inject dependency " + target.toString() + " into " + provider.toString());
    }
}
