package org.terasology.gestalt.di.instance;

import org.terasology.gestalt.di.BeanContext;
import org.terasology.gestalt.di.BeanEnvironment;
import org.terasology.gestalt.di.BeanKey;
import org.terasology.gestalt.di.Lifetime;

import java.util.Optional;
import java.util.function.Supplier;

public class SupplierProvider<T> extends BeanProvider<T> {
    private final Supplier<T> supplier;

    public SupplierProvider(BeanEnvironment environment, Lifetime lifetime, Supplier<T> supplier) {
        super(environment, lifetime);
        this.supplier = supplier;
    }
    @Override
    public Optional<T> get(BeanKey identifier, BeanContext current, BeanContext scopedTo) {
        return Optional.ofNullable(supplier.get());
    }

    @Override
    public void close() throws Exception {
        // nothing to close.
    }
}
