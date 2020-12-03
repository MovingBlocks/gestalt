package org.terasology.gestalt.di;

import org.terasology.context.BeanDefinition;
import org.terasology.gestalt.di.qualifiers.Qualifier;

import java.lang.annotation.Annotation;
import java.util.Optional;

public interface BeanContext {

    Optional<BeanContext> getParent();

    <T> T inject(T instance);

    <T> T inject(BeanKey<T> identifier);

    <T> T getBean(Class<T> clazz);

    <T> T getBean(Class<T> clazz, Qualifier<T> qualifier);

    <T> T getBean(BeanDefinition<T> beanDefinition);
}
