package org.terasology.gestalt.di.instance;

import org.terasology.gestalt.di.BeanContext;
import org.terasology.gestalt.di.BeanEnvironment;
import org.terasology.gestalt.di.BeanIdentifier;
import org.terasology.gestalt.di.BeanTransaction;
import org.terasology.gestalt.di.Lifetime;

import java.util.Optional;

public abstract class BeanProvider<T> implements AutoCloseable {
    protected final Lifetime lifetime;
    protected final BeanEnvironment environment;

    public BeanProvider(BeanEnvironment environment, Lifetime lifetime) {
        this.lifetime = lifetime;
        this.environment = environment;
    }

    public Lifetime getLifetime() {
        return lifetime;
    }

    public abstract T get(BeanIdentifier identifier, BeanContext current, BeanContext scopedTo, BeanTransaction transaction);

}
