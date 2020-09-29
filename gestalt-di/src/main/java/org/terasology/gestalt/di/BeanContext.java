package org.terasology.gestalt.di;

import com.sun.istack.internal.NotNull;

import java.lang.annotation.Annotation;
import java.util.Optional;

public interface BeanContext {

    Optional<BeanContext> getRoot();

    <T> T inject(Annotation parent, @NotNull T instance);

    <T> T inject(BeanIdentifier definition);

}
