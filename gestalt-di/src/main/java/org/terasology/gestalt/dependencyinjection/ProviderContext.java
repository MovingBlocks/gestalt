package org.terasology.gestalt.dependencyinjection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.gestalt.module.ModuleEnvironment;
import org.terasology.gestalt.util.collection.TypeKeyedMap;

import java.util.Set;
import java.util.stream.Collectors;


public class ProviderContext {

    private Logger logger = LoggerFactory.getLogger(ProviderContext.class);
    private TypeKeyedMap<Object> globalProviders = new TypeKeyedMap<>();
    private Set<Object> systems;

    public <T> void addProvider(Class<T> interfaceType, T provider) {
        globalProviders.put(interfaceType, provider);
    }

    public Set<Object> getSystems() {
        return systems;
    }

    public void generateForEnvironment(ModuleEnvironment newEnvironment) {
        TypeKeyedMap<Object> providerMap = new TypeKeyedMap<>();
        for (Class<? extends Provider> providerType : newEnvironment.getSubtypesOf(Provider.class)) {
            try {
                Provider provider = providerType.newInstance();
                Class implementedInterface = provider.providerFor();
                providerMap.put(implementedInterface, provider);
            } catch (InstantiationException | IllegalAccessException e) {
                logger.error("Failed to generate system {}", providerType, e);
            }
        }
        providerMap.putAll(globalProviders);
        providerMap.values().stream().filter(x -> x instanceof Provider).map(x -> (Provider) x).forEach(
                x -> x.init(providerMap)
        );

        systems = providerMap.values().stream().filter(x -> x instanceof Provider).map(x -> (Provider) x).flatMap(x -> x.getAllSystems().stream()).collect(Collectors.toSet());
    }

}
