package org.terasology.gestalt.di;

import org.terasology.context.Argument;
import org.terasology.context.BeanDefinition;
import org.terasology.context.BeanResolution;
import org.terasology.context.exception.DependencyInjectionException;

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
        BeanKey<T> key = BeanUtilities.resolveBeanKey(target, argument);
        return beanContext.findBean(key);
    }

    @Override
    public <T> Optional<T> resolveParameterArgument(Class<T> target, Argument<T> argument)  throws DependencyInjectionException {
        BeanKey<T> key = BeanUtilities.resolveBeanKey(target, argument);
        return beanContext.findBean(key);
    }
}
