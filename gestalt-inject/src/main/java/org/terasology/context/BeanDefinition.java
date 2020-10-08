package org.terasology.context;

public interface BeanDefinition<T> {
    AnnotationMetadata getAnnotationMetadata();

//    boolean isSingleton();

    Class<T> targetClass();
}
