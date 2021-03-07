package org.terasology.gestalt.di.function;

public interface Function2<T1, T2, R> extends BeanFunction {
    R apply(T1 t1, T2 t2);
}
