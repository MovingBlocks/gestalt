package org.terasology.context;

public interface BeanDefinition<T> {

    boolean isSingleton();

    Class<T> targetClass();
}
