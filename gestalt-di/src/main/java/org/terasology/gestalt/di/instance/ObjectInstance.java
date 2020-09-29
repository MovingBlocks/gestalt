package org.terasology.gestalt.di.instance;

import org.terasology.gestalt.di.BeanContext;

public class ObjectInstance extends Instance {

    private Object service;

    public ObjectInstance(Class<?> serviceType, Object service) {

    }

    @Override
    public Object resolve(BeanContext beanContext) {
        return null;
    }

    @Override
    public void close() throws Exception {

    }
}
