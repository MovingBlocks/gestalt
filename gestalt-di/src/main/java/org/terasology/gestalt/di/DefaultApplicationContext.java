package org.terasology.gestalt.di;

import java.util.ArrayList;
import java.util.List;

public class DefaultApplicationContext extends DefaultBeanContext {
    LifecycleContainer rootLifecycle;
    private List<LifecycleContainer> lifecycles = new ArrayList<>();

    public void registerLifecycle( Lifecycle lifecycle){
        lifecycles.add(new LifecycleContainer(this));
    }

    public void getLifecycle() {

    }

    public DefaultApplicationContext() {
        this.rootLifecycle = new LifecycleContainer();
    }
}
