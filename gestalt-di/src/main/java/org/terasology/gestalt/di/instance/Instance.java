package org.terasology.gestalt.di.instance;

import org.terasology.gestalt.di.BeanContext;
import org.terasology.gestalt.di.BeanEnvironment;
import org.terasology.gestalt.di.BeanIdentifier;
import org.terasology.gestalt.di.Lifetime;
import org.terasology.gestalt.di.ServiceGraph;

import java.util.Optional;

public abstract class Instance<T> implements AutoCloseable {
    protected final Lifetime lifetime;
    protected final BeanEnvironment environment;
    protected final ServiceGraph serviceGraph;

    public Instance(BeanEnvironment environment, Lifetime lifetime, ServiceGraph serviceGraph) {
        this.lifetime = lifetime;
        this.environment = environment;
        this.serviceGraph = serviceGraph;
    }

    public abstract Optional<T> get(BeanIdentifier identifier, BeanContext context);

}
