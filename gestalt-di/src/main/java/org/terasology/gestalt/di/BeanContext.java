package org.terasology.gestalt.di;

import java.lang.annotation.Annotation;
import java.util.Optional;

public interface BeanContext {

    Optional<BeanContext> getRoot();

    <T> T inject(Annotation parent,T instance);

    <T> T inject(BeanIdentifier definition);

}
