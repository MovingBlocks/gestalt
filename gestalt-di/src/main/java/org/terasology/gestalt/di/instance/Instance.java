package org.terasology.gestalt.di.instance;

import org.terasology.gestalt.di.BeanContext;

import java.util.function.Function;

public abstract class Instance implements AutoCloseable{
    protected Class serviceType;
    protected Class implementationType;

    protected Instance(Class serviceType, Class implementationType){
        this.serviceType = serviceType;
        this.implementationType = implementationType;
    }

    public abstract Function<BeanContext,Object> toResolve(BeanContext from);
}
