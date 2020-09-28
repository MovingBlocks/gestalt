package org.terasology.gestalt.di;

import org.terasology.context.BeanContext;

public class LifecycleContainer extends DefaultBeanContext {
    private Lifecycle lifecycle;
    private LifecycleContainer parent;
    private DefaultApplicationContext applicationContext;

    public LifecycleContainer(DefaultApplicationContext applicationContext){
        this.applicationContext = applicationContext;
    }

    @Override
    public BeanContext getParent() {
        return this.parent;
    }


    public LifecycleContainer() {
    }
}
