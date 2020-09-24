package org.terasology.gestalt.di;

import org.terasology.context.BeanDefinition;

import java.lang.annotation.Annotation;

public class StereotypeLifecycle<T extends Annotation> extends AbstractLifecycle{
    private Class<T> stereotype;

    public StereotypeLifecycle(Class<T> stereotype){
        this.stereotype = stereotype;
    }

    @Override
    public boolean isIn(BeanDefinition definition) {
        return definition.getAnnotationMetadata().hasStereotype(stereotype);
    }
}
