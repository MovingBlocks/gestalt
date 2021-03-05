package org.terasology.gestalt.di.instance;

import org.terasology.context.AbstractBeanDefinition;
import org.terasology.context.BeanDefinition;
import org.terasology.gestalt.di.BeanContext;
import org.terasology.gestalt.di.BeanEnvironment;
import org.terasology.gestalt.di.BeanKey;
import org.terasology.gestalt.di.DefaultBeanResolution;
import org.terasology.gestalt.di.Lifetime;
import org.terasology.gestalt.di.function.Function0;

import java.util.Optional;
import java.util.function.Supplier;

public class SupplierProvider<T> extends BeanProvider<T> {
    private final Function0<T> supplier;
    private final Class<T> target;

    public SupplierProvider(BeanEnvironment environment, Lifetime lifetime, Class<T> target, Function0<T> supplier) {
        super(environment, lifetime);
        this.supplier = supplier;
        this.target = target;
    }
    @Override
    public Optional<T> get(BeanKey identifier, BeanContext current, BeanContext scopedTo) {
        Optional<T> result = Optional.ofNullable(supplier.apply());
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
