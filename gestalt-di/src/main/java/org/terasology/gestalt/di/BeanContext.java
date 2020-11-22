package org.terasology.gestalt.di;

import java.lang.annotation.Annotation;
import java.util.Optional;

public interface BeanContext {

    Optional<BeanContext> getParent();

    <T> T inject(Annotation parent,T instance);

    <T> T inject(BeanKey<T> identifier);


}
