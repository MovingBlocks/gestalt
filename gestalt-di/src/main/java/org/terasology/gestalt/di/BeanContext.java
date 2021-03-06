package org.terasology.gestalt.di;

import org.terasology.context.Argument;
import org.terasology.gestalt.di.injection.Qualifier;

import java.util.List;
import java.util.Optional;

public interface BeanContext {

    Optional<BeanContext> getParent();

    <T> Optional<T> inject(T instance);

    <T> T getBean(BeanKey<T> identifier);

    <T> T getBean(Class<T> clazz);

    <T> T getBean(Class<T> clazz, Qualifier qualifier);

    <T> List<T> getBeans(BeanKey<T> identifier);

    <T> List<T> getBeans(Class<T> clazz);

    <T> List<T> getBeans(Class<T> clazz, Qualifier qualifier);

    <T> Optional<T> findBean(BeanKey<T> identifier);

    <T> Optional<T> findBean(Class<T> clazz);

    <T> Optional<T> findBean(Class<T> clazz, Qualifier qualifier);

    BeanContext getNestedContainer();

    BeanContext getNestedContainer(ServiceRegistry... registries);
}
