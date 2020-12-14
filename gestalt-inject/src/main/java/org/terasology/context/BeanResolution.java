package org.terasology.context;

import org.terasology.context.exception.DependencyInjectionException;

import java.util.Optional;

public interface BeanResolution {
    <T> Optional<T> resolveConstructorArgument(Class<T> target, Argument<T> argument) throws DependencyInjectionException;

    <T> Optional<T> resolveParameterArgument(Class<T> target, Argument<T> argument) throws DependencyInjectionException;
}
