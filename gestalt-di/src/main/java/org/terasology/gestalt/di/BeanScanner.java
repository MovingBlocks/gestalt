package org.terasology.gestalt.di;

public interface BeanScanner {
    void apply(ServiceRegistry registry, BeanEnvironment environment);
}
