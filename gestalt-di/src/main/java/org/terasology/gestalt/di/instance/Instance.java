package org.terasology.gestalt.di.instance;

import org.terasology.gestalt.di.BeanContext;
import org.terasology.gestalt.di.BeanIdentifier;
import org.terasology.gestalt.di.Lifetime;
import org.terasology.gestalt.di.ServiceGraph;

import java.util.Optional;
import java.util.function.Function;

public abstract class Instance<T> implements AutoCloseable {
    protected Lifetime lifetime;
    public Instance(Lifetime lifetime) {
        this.lifetime = lifetime;
    }

    public abstract Optional<T> get(BeanIdentifier identifier, BeanContext context);

}
