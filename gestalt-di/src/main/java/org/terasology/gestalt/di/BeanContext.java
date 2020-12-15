package org.terasology.gestalt.di;

import org.terasology.gestalt.di.qualifiers.Qualifier;

import java.util.Optional;

public interface BeanContext {

    Optional<BeanContext> getParent();

    <T> Optional<T> inject(T instance);

    <T> Optional<T> inject(T instance, BeanTransaction transaction);

    <T> Optional<T> getBean(BeanKey<T> identifier);

    <T> Optional<T> getBean(BeanKey<T> identifier, BeanTransaction transaction);

    <T> Optional<T> getBean(Class<T> clazz);

    <T> Optional<T> getBean(Class<T> clazz, BeanTransaction transaction);

    <T> Optional<T> getBean(Class<T> clazz, Qualifier<T> qualifier);

    <T> Optional<T> getBean(Class<T> clazz, Qualifier<T> qualifier, BeanTransaction transaction);


}
