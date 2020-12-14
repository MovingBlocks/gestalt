package org.terasology.gestalt.di;

import org.terasology.context.BeanDefinition;
import org.terasology.gestalt.di.qualifiers.Qualifier;

import java.lang.annotation.Annotation;
import java.util.Optional;

public interface BeanContext {

    Optional<BeanContext> getParent();

    <T> T inject(T instance) throws Exception;

    <T> T inject(T instance, BeanTransaction transaction);

    <T> T getBean(BeanKey<T> identifier) throws Exception;

    <T> T getBean(BeanKey<T> identifier, BeanTransaction transaction) throws Exception;

    <T> T getBean(Class<T> clazz) throws Exception;

    <T> T getBean(Class<T> clazz, BeanTransaction transaction) throws Exception;

    <T> T getBean(Class<T> clazz, Qualifier<T> qualifier) throws Exception;

    <T> T getBean(Class<T> clazz, Qualifier<T> qualifier, BeanTransaction transaction) throws Exception;


}
