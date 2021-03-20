// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.gestalt.di.instance;

import org.terasology.context.AbstractBeanDefinition;
import org.terasology.context.BeanDefinition;
import org.terasology.gestalt.di.BeanContext;
import org.terasology.gestalt.di.BeanEnvironment;
import org.terasology.gestalt.di.BeanKey;
import org.terasology.gestalt.di.DefaultBeanResolution;
import org.terasology.context.Lifetime;

import java.util.Optional;
import java.util.function.Supplier;

public class SupplierProvider<T> extends BeanProvider<T> {
    private final Supplier<T> supplier;
    private final Class<T> target;

    public SupplierProvider(BeanEnvironment environment, Lifetime lifetime, Class<T> target, Supplier<T> supplier) {
        super(environment, lifetime);
        this.supplier = supplier;
        this.target = target;
    }
    @Override
    public Optional<T> get(BeanKey identifier, BeanContext current, BeanContext scopedTo) {
        Optional<T> result = Optional.ofNullable(supplier.get());
        result.ifPresent(k -> {
            BeanDefinition<T> definition = (BeanDefinition<T>)environment.getDefinition(target);
            if (definition instanceof AbstractBeanDefinition) {
                BeanContext cntx = lifetime == Lifetime.Singleton ? current : scopedTo;
                definition.inject(k, new DefaultBeanResolution(cntx, environment));
            }
        });
        return result;
    }

    @Override
    public void close() throws Exception {
        // nothing to close.
    }
}
