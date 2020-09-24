package org.terasology.gestalt.di;

import org.terasology.context.BeanDefinition;

public abstract class AbstractLifecycle implements Lifecycle {

    public abstract boolean isIn(BeanDefinition definition);

    

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }
}
