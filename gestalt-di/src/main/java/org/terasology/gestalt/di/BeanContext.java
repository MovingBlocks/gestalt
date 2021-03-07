// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.gestalt.di;

import org.terasology.gestalt.di.injection.Qualifier;

import java.util.List;
import java.util.Optional;

/**
 * A context contains information about resolving an instance and a collection of objects that are bound
 * to this context.
 */
public interface BeanContext extends AutoCloseable {

    /**
     * The containing context that this context is parented to
     * @return bean context
     */
    Optional<BeanContext> getParent();

    /**
     * resolve and inject into fields annotated by {@link javax.inject.Inject} for the object provided in instance.
     *
     * returns an empty optional if the target object is not processed by gestalt-inject-java
     *
     * @param instance the objects to inject into
     * @param <T>
     * @throws org.terasology.context.exception.BeanNotFoundException if a bean definition is not resolved for the injected target
     * @return the instance
     */
    <T> T inject(T instance);

    /**
     * resolve object by its {@link BeanKey}
     * @param identifier An identifier that drives the lookup of the target
     * @param <T>
     * @return the resolved object
     */
    <T> T getBean(BeanKey<T> identifier);

    /**
     * resolve object by its {@link Class}
     * @param clazz target class for the lookup
     * @param <T>
     * @return the resolved object
     */
    <T> T getBean(Class<T> clazz);

    /**
     * resolve object by its {@link Class} and {@link Qualifier}
     * @param clazz target class for the lookup
     * @param qualifier the qualifier to filter by
     * @param <T>
     * @return the resolved object
     */
    <T> T getBean(Class<T> clazz, Qualifier qualifier);

    /**
     * resolve multiple target by {@link BeanKey}.
     *
     * @param identifier target identifier for the lookup
     * @param <T>
     * @return multiple resolved object
     */
    <T> List<T> getBeans(BeanKey<T> identifier);

    /**
     * resolve multiple target by {@link Class}.
     *
     * @param clazz target class for the lookup
     * @param <T>
     * @return multiple resolved object
     */
    <T> List<T> getBeans(Class<T> clazz);

    /**
     * resolve multiple target by {@link Class} and {@link Qualifier}.
     *
     * @param clazz target class for the lookup
     * @param qualifier the qualifier to filer by
     * @param <T>
     * @return multiple resolved object
     */
    <T> List<T> getBeans(Class<T> clazz, Qualifier qualifier);

    /**
     * tries to resolve a target object from its identifier. returns an empty optional if
     * a bean is not found.
     *
     * @param identifier an identifier that drives the lookup of the target
     * @param <T>
     * @return an instance of a given target
     */
    <T> Optional<T> findBean(BeanKey<T> identifier);

    /**
     * tries to resolve a target object from its class. returns an empty optional if
     * a bean is not found.
     *
     * @param clazz lookup by class
     * @param <T> target
     * @return the resolved object
     */
    <T> Optional<T> findBean(Class<T> clazz);

    /**
     * tries to resolve a target object from its {@link Class<T>} and {@link Qualifier}. returns an empty optional if
     * a bean is not found.
     *
     * @param clazz lookup by class
     * @param qualifier qualifier to filer multiple implementations by
     * @param <T> target
     * @return the resolved object
     */
    <T> Optional<T> findBean(Class<T> clazz, Qualifier qualifier);

    /**
     * a new context that is a child of this context
     * @return a child {@link BeanContext}
     */
    BeanContext getNestedContainer();

    /**
     *  a new context that is a child of this context and a list of {@link ServiceRegistry} that describe the child {@link BeanContext}
     * @param registries that define the context
     * @return a child {@link BeanContext}
     */
    BeanContext getNestedContainer(ServiceRegistry... registries);

    /**
     * the environment that drives the context
     * @return the environment
     */
    BeanEnvironment getEnvironment();
}
