package org.terasology.gestalt.di;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.terasology.context.BeanDefinition;
import org.terasology.context.SoftServiceLoader;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class BeanEnvironment {
    private final Map<Class<?>, BeanDefinition<?>> definitions = new HashMap<>();
    private final Multimap<ClassLoader, Class<?>> classMapping = ArrayListMultimap.create();
    private final Multimap<Class<?>, Class<?>> interfaceIndex = ArrayListMultimap.create();

    public BeanEnvironment() {
        loadDefinitions(BeanEnvironment.class.getClassLoader());
    }

    public void loadDefinitions(ClassLoader loader) {
        SoftServiceLoader<BeanDefinition> definitions = new SoftServiceLoader<>(BeanDefinition.class, loader);
        for (BeanDefinition<?> definition : definitions) {
            this.classMapping.put(loader, definition.targetClass());
            for (Class<?> clazzInterface : definition.targetClass().getInterfaces()) {
                interfaceIndex.put(clazzInterface, definition.targetClass());
            }
            this.definitions.put(definition.targetClass(), definition);
        }
    }

    public <T> Iterator<Class<? extends T>> implementedClasses(Class<T> cls) {
        if (cls.isInterface()) {
            return Collections.emptyIterator();
        }
        return (Iterator) interfaceIndex.get(cls).iterator();
    }

    public void releaseDefinitions(ClassLoader loader) {
        Set<Class<?>> toRelease = new HashSet<>();
        for (BeanDefinition<?> definition : definitions.values()) {
            if (definition.targetClass().getClassLoader().equals(loader)) {
                toRelease.add(definition.targetClass());
            }
        }
    }

    public <T> BeanDefinition<T> getDefinitions(Class<T> beanType) {
        return (BeanDefinition<T>) definitions.get(beanType);
    }

}
