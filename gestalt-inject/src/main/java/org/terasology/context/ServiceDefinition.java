package org.terasology.context;

public interface ServiceDefinition<T> {
    String getName();

    T load();
}
