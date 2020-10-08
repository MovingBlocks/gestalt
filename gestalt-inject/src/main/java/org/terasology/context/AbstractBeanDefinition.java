package org.terasology.context;

import java.beans.beancontext.BeanContext;

public abstract class AbstractBeanDefinition<T> implements BeanDefinition<T> {

    public AnnotationMetadata getAnnotationMetadata() {
        return new DefaultAnnotationMetadata(new AnnotationValue[]{});
    }

//    public T build(BeanContext beanContext) {
//        return null;
//    }

    public T build(BeanResolution resolution) {
        return null;
    }


    public abstract Class<T> targetClass();
}
