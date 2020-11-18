package org.terasology.gestalt.di.instance;

import org.terasology.gestalt.di.BeanContext;
import org.terasology.gestalt.di.BeanEnvironment;
import org.terasology.gestalt.di.BeanIdentifier;
import org.terasology.gestalt.di.Lifetime;
import org.terasology.gestalt.di.ServiceGraph;

import java.util.Optional;

/**
 * Implementation from class type. use environment to work out fields and properties to inject.
 */
public class ClassInstance<T> extends Instance<T> {
    private final Class<T> target;

    public ClassInstance(BeanEnvironment environment, Lifetime lifetime, ServiceGraph serviceGraph, Class<T> target) {
        super(environment, lifetime, serviceGraph);
        this.target = target;
        environment.getInstance(target);
    }

    @Override
    public Optional<T> get(BeanIdentifier identifier, BeanContext context) {
        switch (lifetime) {
            case Scoped:
                break;
            case Singleton:
                break;
            case Transient:
                break;
        }
        return Optional.empty();
    }

    @Override
    public void close() throws Exception {

    }
}
