package org.terasology.gestalt.di;

import org.terasology.context.BeanDefinition;

import javax.inject.Singleton;

public interface Lifecycle {
    void start();
    void stop();
}
