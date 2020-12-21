package org.terasology.gestalt.di.instance;

import org.terasology.context.AbstractBeanDefinition;
import org.terasology.context.Argument;
import org.terasology.context.BeanDefinition;
import org.terasology.context.BeanResolution;
import org.terasology.gestalt.di.BeanContext;
import org.terasology.gestalt.di.BeanEnvironment;
import org.terasology.gestalt.di.BeanIdentifier;
import org.terasology.gestalt.di.BeanKey;
import org.terasology.gestalt.di.BeanKeys;
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
        environment.getDefinition(target);
    }

    @Override
    public Optional<T> get(BeanIdentifier identifier, BeanContext current, BeanContext scopedTo) {
        BeanDefinition<T> definition = environment.getDefinition(target);
        if (definition instanceof AbstractBeanDefinition) {
            BeanContext cntx = lifetime == Lifetime.Singleton ? current : scopedTo;
            return (definition).build(new BeanResolution() {
                @Override
                public <T> Optional<T> resolveConstructorArgument(Class<T> target, Argument<T> argument) {
                    BeanKey<T> key = BeanKeys.resolveBeanKey(argument.getType(), argument);
                    return cntx.getBean(key);
                }

                @Override
                public <T> Optional<T> resolveParameterArgument(Class<T> target, Argument<T> argument) {
                    BeanKey<T> key = BeanKeys.resolveBeanKey(argument.getType(), argument);
                    return cntx.getBean(key);
                }
            });
        }
        return Optional.empty();
    }

    @Override
    public void close() throws Exception {
        // Noting to close
    }
}
