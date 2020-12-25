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
        BeanKey<?> key = BeanUtilities.resolveBeanKey(target, argument);
        Optional<T> result = Optional.empty();
        if(argument.getType().isInterface()) {
            for(BeanDefinition<?> def: environment.byInterface(argument.getType())) {
                key = BeanUtilities.resolveBeanKey(def.targetClass(), argument);
                Optional<T> tg = (Optional<T>) beanContext.getBean(key);
                if(tg.isPresent() && result.isPresent()) {
                    throw new DependencyInjectionException("multiple possible beans resolved");
                }
                result = tg;
            }
        } else {
            result =  (Optional<T>) beanContext.getBean(key);
        }
        return result;
    }

    @Override
    public <T> Optional<T> resolveParameterArgument(Class<T> target, Argument<T> argument)  throws DependencyInjectionException{
        BeanKey<?> key = BeanUtilities.resolveBeanKey(target, argument);
        Optional<T> result = Optional.empty();
        if(argument.getType().isInterface()) {
            for(BeanDefinition<?> def: environment.byInterface(argument.getType())) {
                key = BeanUtilities.resolveBeanKey(def.targetClass(), argument);
                Optional<T> tg = (Optional<T>) beanContext.getBean(key);
                if(tg.isPresent() && result.isPresent()) {
                    throw new DependencyInjectionException("multiple possible beans resolved");
                }
                result = tg;
            }
        } else {
            result =  (Optional<T>) beanContext.getBean(key);
        }
        return result;
    }
}
