package org.terasology.gestalt.di.function;

import org.terasology.context.AbstractBeanDefinition;
import org.terasology.context.Argument;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class FunctionBeanDefinition<T> extends AbstractBeanDefinition<T> {
    Argument[] arguments;

    public FunctionBeanDefinition(BeanFunction function) throws NoSuchMethodException {
        if (function instanceof Function1) {
            Method method = function.getClass().getMethod("apply", Function1.class);
            Parameter[] parameters = method.getParameters();

            arguments = new Argument[]{
                    new ReflectionArgument(parameters[0])
            };
        } else if (function instanceof Function2) {
            Method method = function.getClass().getMethod("apply", Function2.class);
            Parameter[] parameters = method.getParameters();
            arguments = new Argument[]{
                    new ReflectionArgument(parameters[0]),
                    new ReflectionArgument(parameters[1])
            };
        } else if (function instanceof Function3) {
            Method method = function.getClass().getMethod("apply", Function2.class);
            Parameter[] parameters = method.getParameters();
            arguments = new Argument[]{
                    new ReflectionArgument(parameters[0]),
                    new ReflectionArgument(parameters[1]),
                    new ReflectionArgument(parameters[2])
            };
        } else if (function instanceof Function4) {
            Method method = function.getClass().getMethod("apply", Function2.class);
            Parameter[] parameters = method.getParameters();
            arguments = new Argument[]{
                    new ReflectionArgument(parameters[0]),
                    new ReflectionArgument(parameters[1]),
                    new ReflectionArgument(parameters[2]),
                    new ReflectionArgument(parameters[3])
            };
        } else if (function instanceof Function5) {
            Method method = function.getClass().getMethod("apply", Function2.class);
            Parameter[] parameters = method.getParameters();
            arguments = new Argument[]{
                    new ReflectionArgument(parameters[0]),
                    new ReflectionArgument(parameters[1]),
                    new ReflectionArgument(parameters[2]),
                    new ReflectionArgument(parameters[3]),
                    new ReflectionArgument(parameters[4])
            };
        } else if (function instanceof Function6) {
            Method method = function.getClass().getMethod("apply", Function2.class);
            Parameter[] parameters = method.getParameters();
            arguments = new Argument[]{
                    new ReflectionArgument(parameters[0]),
                    new ReflectionArgument(parameters[1]),
                    new ReflectionArgument(parameters[2]),
                    new ReflectionArgument(parameters[3]),
                    new ReflectionArgument(parameters[4]),
                    new ReflectionArgument(parameters[5])
            };
        } else if (function instanceof Function7) {
            Method method = function.getClass().getMethod("apply", Function2.class);
            Parameter[] parameters = method.getParameters();
            arguments = new Argument[]{
                    new ReflectionArgument(parameters[0]),
                    new ReflectionArgument(parameters[1]),
                    new ReflectionArgument(parameters[2]),
                    new ReflectionArgument(parameters[3]),
                    new ReflectionArgument(parameters[4]),
                    new ReflectionArgument(parameters[5]),
                    new ReflectionArgument(parameters[6])
            };
        } else if (function instanceof Function8) {
            Method method = function.getClass().getMethod("apply", Function2.class);
            Parameter[] parameters = method.getParameters();
            arguments = new Argument[]{
                    new ReflectionArgument(parameters[0]),
                    new ReflectionArgument(parameters[1]),
                    new ReflectionArgument(parameters[2]),
                    new ReflectionArgument(parameters[3]),
                    new ReflectionArgument(parameters[4]),
                    new ReflectionArgument(parameters[5]),
                    new ReflectionArgument(parameters[6]),
                    new ReflectionArgument(parameters[7])
            };
        }
    }

    @Override
    public Argument[] getArguments() {
        return arguments;
    }

    @Override
    public Class targetClass() {
        return null;
    }
}
