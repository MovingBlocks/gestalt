package org.terasology.gestalt.di.instance;

import org.terasology.gestalt.di.BeanContext;

import java.util.function.Function;

public class ObjectInstance extends Instance {

    private Object service;

    public ObjectInstance(Class<?> serviceType, Object service) {
        super(null,null);
    }

    @Override
    public void close() throws Exception {

    }

    @Override
    public Function<BeanContext, Object> toResolve(BeanContext from) {
        return null;
    }
}
