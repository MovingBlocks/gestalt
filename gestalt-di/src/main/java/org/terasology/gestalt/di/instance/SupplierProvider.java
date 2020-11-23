package org.terasology.gestalt.di.instance;

import org.terasology.gestalt.di.BeanContext;
import org.terasology.gestalt.di.BeanEnvironment;
import org.terasology.gestalt.di.BeanIdentifier;
import org.terasology.gestalt.di.Lifetime;

import java.util.function.Supplier;

public class SupplierProvider<T> extends BeanProvider<T> {
    private Supplier<T> supplier;

    public SupplierProvider(BeanEnvironment environment, Lifetime lifetime, Supplier<T> supplier) {
        super(environment, lifetime);
        this.supplier = supplier;
    }

    @Override
    public T get(BeanIdentifier identifier, BeanContext current, BeanContext scopedTo) {
        return supplier.get();
    }

    @Override
    public void close() throws Exception {

    }
}
