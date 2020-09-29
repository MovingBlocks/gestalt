package org.terasology.gestalt.di;

import org.terasology.context.BeanDefinition;
import org.terasology.context.SoftServiceLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceGraph {
    private Map<Class, String> families = new HashMap<>();
    private final Map<Class, BeanDefinition> definitions = new HashMap<>();
    private final Map<ClassLoader, List<Class>> classMapping = new HashMap<>();

    public ServiceGraph() {

    }

    public void start() {

    }

    public void loadDefinitions(ClassLoader loader) {
        List<Class> cl = new ArrayList<>();
        SoftServiceLoader<BeanDefinition> definitions = new SoftServiceLoader<BeanDefinition>(BeanDefinition.class, loader);
        for (BeanDefinition definition : definitions) {
            cl.add(definition.targetClass());
            this.definitions.put(definition.targetClass(), definition);
        }
        this.classMapping.put(loader,cl);
    }

}
