package org.terasology.gestalt.di;

import org.terasology.context.BeanDefinition;
import org.terasology.context.exception.DependencyInjectionException;
import org.terasology.gestalt.di.qualifiers.Qualifier;

import java.lang.annotation.Annotation;
import java.util.Optional;

public interface BeanContext {

    Optional<BeanContext> getParent();

    <T> Optional<T> inject(T instance) throws Exception;

    <T> Optional<T> inject(T instance, BeanTransaction transaction);

    <T> Optional<T> getBean(BeanKey<T> identifier) throws DependencyInjectionException;

    <T> Optional<T> getBean(BeanKey<T> identifier, BeanTransaction transaction) throws DependencyInjectionException;

    <T> Optional<T> getBean(Class<T> clazz) throws DependencyInjectionException;

    <T> Optional<T> getBean(Class<T> clazz, BeanTransaction transaction) throws DependencyInjectionException;

    <T> Optional<T> getBean(Class<T> clazz, Qualifier<T> qualifier) throws DependencyInjectionException;

    <T> Optional<T> getBean(Class<T> clazz, Qualifier<T> qualifier, BeanTransaction transaction) throws DependencyInjectionException;


}
