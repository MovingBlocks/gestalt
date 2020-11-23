package org.terasology.gestalt.di;

import java.lang.annotation.Annotation;
import java.util.Optional;

public interface BeanContext {

    Optional<BeanContext> getParent();

    <T> T inject(T instance);

    <T> T inject(BeanKey<T> identifier);

    <T> T fetch(Class<T> clazz);
}
