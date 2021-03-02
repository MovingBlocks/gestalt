package org.terasology.gestalt.di;

import org.terasology.context.Argument;
import org.terasology.context.BeanResolution;

import javax.inject.Provider;
import java.util.Optional;

public class DefaultBeanResolution implements BeanResolution {
    private final BeanContext beanContext;
    private final BeanEnvironment environment;

    public DefaultBeanResolution(BeanContext context, BeanEnvironment environment) {
        this.beanContext = context;
        this.environment = environment;
    }

    @Override
    public <T> Optional<T> resolveConstructorArgument(Class<T> target, Argument<T> argument) {
        return getBean(target, argument);
    }

    @Override
    public <T> Optional<T> resolveParameterArgument(Class<T> target, Argument<T> argument) {
        return getBean(target, argument);
    }

    private <T> Optional<T> getBean(Class<T> target, Argument<T> argument) {
        if (target.equals(Provider.class)) {
            BeanKey<T> key = BeanUtilities.resolveBeanKey(argument.getType(), argument);
            return (Optional<T>) Optional.of((Provider<T>) () -> beanContext.getBean(key));
        } else {
            BeanKey<T> key = BeanUtilities.resolveBeanKey(target, argument);
            return beanContext.findBean(key);
        }

    }
}
