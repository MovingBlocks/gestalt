package org.terasology.gestalt.di;

import org.terasology.gestalt.di.qualifiers.Qualifier;

import java.util.Optional;
import java.util.function.Supplier;

public interface BeanContext {

    Optional<BeanContext> getParent();

    <T> Optional<T> inject(T instance);

    <T> Optional<T> getBean(BeanKey<T> identifier);

    <T> Optional<T> getBean(Class<T> clazz);

    <T> Optional<T> getBean(Class<T> clazz, Qualifier<T> qualifier);

}
