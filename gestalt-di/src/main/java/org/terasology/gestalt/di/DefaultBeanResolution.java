package org.terasology.gestalt.di;

import org.terasology.context.Argument;
import org.terasology.context.BeanResolution;

public class DefaultBeanResolution implements BeanResolution {
    private final BeanContext beanContext;
    public DefaultBeanResolution(BeanContext context) {
        this.beanContext = context;
    }
    @Override
    public <T> T resolveConstructorArgument(Class<T> target, Argument<T> argument) {
        if(target.isInterface()){

        }

        BeanKey<T> key = BeanKeys.resolveBeanKey(argument.getType(), argument);
        return beanContext.resolve(key);
    }

    @Override
    public <T> T resolveParameterArgument(Class<T> target, Argument<T> argument) {
        BeanKey<T> key = BeanKeys.resolveBeanKey(argument.getType(), argument);
        return beanContext.resolve(key);
    }
}
