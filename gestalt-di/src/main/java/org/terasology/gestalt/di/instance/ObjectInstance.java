package org.terasology.gestalt.di.instance;

import org.terasology.gestalt.di.BeanContext;
import org.terasology.gestalt.di.BeanEnvironment;
import org.terasology.gestalt.di.BeanIdentifier;
import org.terasology.gestalt.di.Lifetime;
import org.terasology.gestalt.di.ServiceGraph;

import java.util.Optional;
import java.util.function.Supplier;

public class ObjectInstance<T> extends Instance<T> {
    private Supplier<T> supplier;

    public ObjectInstance(BeanEnvironment environment, Lifetime lifetime, ServiceGraph serviceGraph, Supplier<T> supplier) {
        super(environment, lifetime,serviceGraph);
        this.supplier = supplier;
    }

    @Override
    public Optional<T> get(BeanIdentifier identifier, BeanContext context) {
        T obj;
        switch (lifetime) {
            case Transient:
                return Optional.of(supplier.get());
            case Singleton:
                BeanContext serviceContext = serviceGraph.serviceContext();
                if(serviceContext.contains(identifier)) {
                    return Optional.of(serviceContext.get(identifier));
                }
                obj = supplier.get();
                serviceContext.bind(identifier,obj);
                return Optional.of(obj);
            case Scoped:
                if(context.contains(identifier)) {
                    return Optional.of(context.get(identifier));
                }
                obj = supplier.get();
                context.bind(identifier,obj);
                return Optional.of(obj);
        }
        return Optional.of(supplier.get());
    }

    @Override
    public void close() throws Exception {

    }
}
