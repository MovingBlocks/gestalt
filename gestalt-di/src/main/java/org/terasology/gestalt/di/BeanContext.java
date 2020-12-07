package org.terasology.gestalt.di;

import org.terasology.context.BeanDefinition;
import org.terasology.gestalt.di.qualifiers.Qualifier;

import java.lang.annotation.Annotation;
import java.util.Optional;

public interface BeanContext {

    Optional<BeanContext> getParent();

    <T> T inject(T instance) throws Exception;

    <T> T inject(T instance, BeanTransaction transaction);

    <T> T resolve(BeanKey<T> identifier);

    <T> T resolve(BeanKey<T> identifier, BeanTransaction transaction);

    <T> T getBean(Class<T> clazz);

    <T> T getBean(Class<T> clazz, BeanTransaction transaction);

    <T> T getBean(Class<T> clazz, Qualifier<T> qualifier);

    <T> T getBean(Class<T> clazz, Qualifier<T> qualifier, BeanTransaction transaction);

    <T> T getBean(BeanDefinition<T> beanDefinition);

    <T> T getBean(BeanDefinition<T> beanDefinition, BeanTransaction transaction);

}
