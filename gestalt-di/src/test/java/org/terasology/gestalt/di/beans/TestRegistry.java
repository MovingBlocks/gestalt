package org.terasology.gestalt.di.beans;

import org.terasology.gestalt.di.ServiceRegistry;

public class TestRegistry extends ServiceRegistry {
    public TestRegistry(){
        this.with(Dep4.class);
        this.with(Dep2.class);
        this.with(Dep3.class);
        this.with(Dep1.class);
    }
}
