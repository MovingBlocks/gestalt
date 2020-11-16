package org.terasology.gestalt.di;

import java.lang.annotation.Annotation;
import java.util.Optional;

public interface BeanContext {

    Optional<BeanContext> getRoot();

    <T> T inject(Annotation parent,T instance);

    <T> T inject(BeanIdentifier definition);

    <T> boolean contains(BeanIdentifier id);

    <T> void bind(BeanIdentifier id, T target);

    <T> T get(BeanIdentifier id);

    <T> void release(BeanIdentifier identifier);


}
