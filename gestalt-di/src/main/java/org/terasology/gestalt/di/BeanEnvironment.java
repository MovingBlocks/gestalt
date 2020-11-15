package org.terasology.gestalt.di;

import org.terasology.context.BeanDefinition;
import org.terasology.context.SoftServiceLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BeanEnvironment {
    private Map<Class, String> families = new HashMap<>();
    private final Map<Class, BeanDefinition> definitions = new HashMap<>();
    private final Map<ClassLoader, List<Class>> classMapping = new HashMap<>();

    public BeanEnvironment() {
        loadDefinitions(BeanEnvironment.class.getClassLoader());
    }

    public void loadDefinitions(ClassLoader loader) {
        List<Class> cl = new ArrayList<>();
        SoftServiceLoader<BeanDefinition> definitions = new SoftServiceLoader<>(BeanDefinition.class, loader);
        for (BeanDefinition definition : definitions) {
            cl.add(definition.targetClass());
            this.definitions.put(definition.targetClass(), definition);
        }
        this.classMapping.put(loader, cl);
    }

    public void releaseDefinitions(ClassLoader loader){
        Set<Class> toRelease = new HashSet<>();
        for(BeanDefinition definition: definitions.values()){
            if(definition.targetClass().getClassLoader().equals(loader)){
                toRelease.add(definition.targetClass());
            }
        }
    }

    public <T> BeanDefinition<T> getInstance(Class<T> beanType) {
        return definitions.get(beanType);
    }
}
