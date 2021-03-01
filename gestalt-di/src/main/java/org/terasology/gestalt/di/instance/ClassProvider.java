package org.terasology.gestalt.di.instance;

import org.terasology.context.AbstractBeanDefinition;
import org.terasology.context.BeanDefinition;
import org.terasology.gestalt.di.BeanContext;
import org.terasology.gestalt.di.BeanEnvironment;
import org.terasology.gestalt.di.BeanKey;
import org.terasology.gestalt.di.DefaultBeanResolution;
import org.terasology.gestalt.di.Lifetime;

import java.util.Optional;

/**
 * Implementation from class type. use environment to work out fields and properties to inject.
 */
public class ClassProvider<T> extends BeanProvider<T> {
    private final Class<T> target;

    public ClassProvider(BeanEnvironment environment, Lifetime lifetime, Class<T> target) {
        super(environment, lifetime);
        this.target = target;
    }

    @Override
    public Optional<T> get(BeanKey identifier, BeanContext current, BeanContext scopedTo) {
        BeanDefinition<T> definition = (BeanDefinition<T>)environment.getDefinition(target);
        if (definition instanceof AbstractBeanDefinition) {
            BeanContext cntx = lifetime == Lifetime.Singleton ? current : scopedTo;
            return (Optional<T>) definition.build(new DefaultBeanResolution(cntx, environment));
        }
        return Optional.empty();
    }

    @Override
    public void close() throws Exception {
        // nothing to close
    }
}
