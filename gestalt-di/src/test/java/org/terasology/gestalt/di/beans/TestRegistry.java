package org.terasology.gestalt.di.beans;

import org.terasology.gestalt.di.Lifetime;
import org.terasology.gestalt.di.ServiceRegistry;

import java.util.function.Supplier;

public class TestRegistry extends ServiceRegistry {
    public TestRegistry(){
        this.with(Dep4.class).use(() -> new Dep4());
        this.with(Dep2.class).use(() -> new Dep2());
        this.with(Dep3.class)
            .lifetime(Lifetime.Singleton)
            .named("dep3");
        this.with(Dep1.class);
    }
}
