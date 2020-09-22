package org.terasology.context;

import java.util.HashMap;

public abstract class AbstractBeanDefinition<T> implements BeanDefinition<T> {

    public AnnotationMetadata getAnnotationMetadata() {
        return new DefaultAnnotationMetadata(new HashMap<>());
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    public abstract Class<T> targetClass();
}
