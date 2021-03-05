package org.terasology.gestalt.di.function;

@FunctionalInterface
public interface Function0< R> extends BeanFunction {
    R apply();
}
