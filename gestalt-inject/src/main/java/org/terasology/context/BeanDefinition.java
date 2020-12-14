package org.terasology.context;

import java.util.Optional;

public interface BeanDefinition<T> {
    AnnotationMetadata getAnnotationMetadata();

    Optional<T> build(BeanResolution resolution);

    Optional<T> inject(T instance, BeanResolution resolution);

    Argument[] getArguments();

    Class[] getTypeArgument();

    Class<T> targetClass();

}
