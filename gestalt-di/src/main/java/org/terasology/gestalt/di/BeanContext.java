package org.terasology.gestalt.di;

import com.sun.istack.internal.NotNull;

import java.lang.annotation.Annotation;

public interface BeanContext {

    <T> T inject(Annotation parent, @NotNull T instance);

    <T> T inject(BeanIdentifier<T> definition);
    
}
