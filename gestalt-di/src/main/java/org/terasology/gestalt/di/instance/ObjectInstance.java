package org.terasology.gestalt.di.instance;

import org.terasology.gestalt.di.BeanContext;
import org.terasology.gestalt.di.BeanIdentifier;
import org.terasology.gestalt.di.Lifetime;
import org.terasology.gestalt.di.ServiceGraph;

import java.util.Optional;
import java.util.function.Supplier;

public class ObjectInstance<T> extends Instance<T> {
    private Supplier<T> supplier;
    private BeanContext current;

    public ObjectInstance(Lifetime lifetime, Supplier<T> supplier, BeanContext beanContext) {
        super(lifetime);
        this.supplier = supplier;
        this.current = beanContext;
    }

    @Override
    public Optional<T> get(BeanIdentifier identifier, BeanContext context) {
        T obj;
        switch (lifetime) {
            case Transient:
                return Optional.of(supplier.get());
            case Singleton:
                if(current.contains(identifier)) {
                    return Optional.of(current.get(identifier));
                }
                obj = supplier.get();
                current.bind(identifier,obj);
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
