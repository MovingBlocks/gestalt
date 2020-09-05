package org.terasology.gestalt.dependencyinjection;

import org.terasology.gestalt.util.collection.TypeKeyedMap;

import java.util.List;

/**
 * Common interface for all {@link GameLogicProvider} implementation classes.
 */
public interface Provider {

    void init(TypeKeyedMap<Object> providers);

    Class<?> providerFor();

    List<Object> getAllSystems();
}
