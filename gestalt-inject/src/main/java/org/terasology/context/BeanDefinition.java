package org.terasology.context;

public interface BeanDefinition<T> {
    AnnotationMetadata getAnnotationMetadata();

    T build(BeanResolution resolution);

    T inject(T instance, BeanResolution resolution);

    Argument[] getArguments();

    Class[] getTypeArgument();

    Class<T> targetClass();

}
