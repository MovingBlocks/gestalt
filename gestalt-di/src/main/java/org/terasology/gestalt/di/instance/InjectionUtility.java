package org.terasology.gestalt.di.instance;

import org.terasology.context.Argument;
import org.terasology.gestalt.di.BeanEnvironment;
import org.terasology.gestalt.di.BeanKey;

public final class InjectionUtility {
    private InjectionUtility() {
    }

    public static void propertyInject(BeanEnvironment environment) {

    }

    public static void constructorInjection() {

    }

    public static <T> BeanKey<T> resolveBeanKey(Argument<T> argument){
        return new BeanKey<T>(argument.getType());
    }
}
