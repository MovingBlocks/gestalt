package org.terasology.gestalt.di.instance;

import org.terasology.context.Argument;
import org.terasology.context.DefaultArgument;
import org.terasology.gestalt.di.BeanContext;
import org.terasology.gestalt.di.BeanEnvironment;
import org.terasology.gestalt.di.BeanKey;
import org.terasology.gestalt.di.Lifetime;
import org.terasology.gestalt.di.function.BeanFunction;
import org.terasology.gestalt.di.function.Function0;
import org.terasology.gestalt.di.function.Function1;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Optional;

public class FunctionProvider<T> extends BeanProvider<T>  {
    BeanFunction function;
    Argument[] args;
    public FunctionProvider(BeanEnvironment environment, Lifetime lifetime, BeanFunction function) throws NoSuchMethodException {
        super(environment, lifetime);
        this.function = function;

        if(function instanceof Function1) {
            Method method = function.getClass().getMethod("apply", Function0.class);
//            method.getReturnType();
            Parameter[] parameters = method.getParameters();
            args = new Argument[]{
                    new DefaultArgument(parameters.getClass(), null)
            };
        }
    }

    @Override
    public Optional<T> get(BeanKey identifier, BeanContext current, BeanContext scopedTo) {
        return Optional.empty();


    }

    @Override
    public void close() throws Exception {

    }
}
