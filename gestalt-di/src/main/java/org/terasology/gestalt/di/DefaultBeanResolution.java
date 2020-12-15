package org.terasology.gestalt.di;

import org.terasology.context.Argument;
import org.terasology.context.BeanResolution;
import org.terasology.context.exception.DependencyInjectionException;

import java.util.Optional;

public class DefaultBeanResolution implements BeanResolution {
    private final BeanContext beanContext;

    public DefaultBeanResolution(BeanContext context) {
        this.beanContext = context;
    }

    @Override
    public <T> Optional<T> resolveConstructorArgument(Class<T> target, Argument<T> argument) {
        if (target.isInterface()) {

        }

        BeanKey<T> key = BeanKeys.resolveBeanKey(argument.getType(), argument);
        return beanContext.getBean(key);
    }

    @Override
    public <T> Optional<T> resolveParameterArgument(Class<T> target, Argument<T> argument)  throws DependencyInjectionException{
        BeanKey<T> key = BeanKeys.resolveBeanKey(argument.getType(), argument);
            return beanContext.getBean(key);
    }
}
