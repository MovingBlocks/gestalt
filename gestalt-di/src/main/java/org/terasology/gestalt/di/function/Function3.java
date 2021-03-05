package org.terasology.gestalt.di.function;

public interface Function3<T1, T2, T3, R> extends BeanFunction {
    R apply(T1 t1, T2 t2, T3 t3);
}
