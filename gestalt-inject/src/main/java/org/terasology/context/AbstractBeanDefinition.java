package org.terasology.context;

public abstract class AbstractBeanDefinition<T> implements BeanDefinition<T> {

    @Override
    public boolean isSingleton() {
        return false;
    }

    public abstract Class<T> targetClass();
}
