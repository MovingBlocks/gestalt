package org.terasology.gestalt.di.instance;

import org.terasology.gestalt.di.BeanContext;

public abstract class Instance implements AutoCloseable{
    protected Class serviceType;
    protected Class implementationType;

    protected Instance(Class serviceType, Class implementationType){
        this.serviceType = serviceType;
        this.implementationType = implementationType;
    }

    public abstract Object resolve(BeanContext beanContext);
}
