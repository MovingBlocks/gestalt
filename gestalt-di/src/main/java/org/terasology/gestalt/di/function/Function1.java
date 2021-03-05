package org.terasology.gestalt.di.function;

@FunctionalInterface
public interface Function1<T1, R> extends BeanFunction {
    R apply(T1 t1);
}
