package org.terasology.gestalt.di;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.terasology.context.BeanDefinition;
import org.terasology.context.SoftServiceLoader;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class BeanEnvironment {
    private static class ClassLookup {
        private final Map<String, BeanDefinition<?>> definitions = new HashMap<>();
        private final List<String> targetLookup = Lists.newArrayList();
        private final Multimap<Class<?>, Class<?>> interfaceIndex = ArrayListMultimap.create();
    }

    public static final ClassLoader BaseClassLoader = BeanEnvironment.class.getClassLoader();
    private final Map<ClassLoader, ClassLookup> beanLookup = new HashMap<>();
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.readLock();


    public BeanEnvironment() {
        loadDefinitions(BeanEnvironment.BaseClassLoader);
    }


    public void loadDefinitions(ClassLoader loader) {
        writeLock.lock();
        try {
            beanLookup.computeIfAbsent(loader, (ld) -> {
                ClassLookup lookup = new ClassLookup();
                SoftServiceLoader<BeanDefinition> definitions = new SoftServiceLoader<>(BeanDefinition.class, ld);
                for (BeanDefinition<?> definition : definitions) {
                    // exclude objects that are from a different classloader
                    if (definition.targetClass().getClassLoader() != loader) {
                        continue;
                    }
                    lookup.targetLookup.add(definition.targetClass().getName());
                    lookup.definitions.put(definition.targetClass().getName(), definition);
                    for (Class<?> clazzInterface : definition.targetClass().getInterfaces()) {
                        lookup.interfaceIndex.put(clazzInterface, definition.targetClass());
                    }
                }
                lookup.targetLookup.sort(Comparator.naturalOrder());
                return lookup;
            });
        } finally {
            writeLock.unlock();
        }
    }

    public void startRead() {
        readLock.lock();
    }

    public void stopRead() {
        readLock.unlock();
    }

    public Iterable<ClassLoader> definitions() {
        return beanLookup.keySet();
    }

    public boolean containsClass(Class<?> clazz) {
        final ClassLookup lookup = beanLookup.get(clazz.getClassLoader());
        if (lookup == null) {
            return false;
        }
        if (!lookup.definitions.containsKey(clazz.getName())) {
            return false;
        }
        return lookup.definitions.get(clazz.getName()).targetClass() == clazz;

    }

    public <T> Iterable<BeanDefinition<T>> definitionsByInterface(Class<T> target) {
        return Iterables.concat(beanLookup.keySet().stream().map(k -> definitionsByInterface(k, target)).collect(Collectors.toList()));
    }

    public <T> Iterable<BeanDefinition<T>> definitionsByInterface(ClassLoader loader, Class<T> target) {
        final ClassLookup lookup = beanLookup.get(loader);
        return lookup.interfaceIndex.get(target).stream().map(k -> (BeanDefinition<T>) getDefinitions(k))::iterator;
    }

    public <T> Iterable<BeanDefinition<T>> definitionsByPrefixAndInterface(ClassLoader loader, String prefix, Class<T> target) {
        return () -> {
            return StreamSupport.stream(definitionsByPrefix(loader, prefix).spliterator(), false).filter(
                k -> {
                    for (Class<?> intr : k.targetClass().getInterfaces()) {
                        if (intr == target) {
                            return true;
                        }
                    }
                    return false;
                }
            ).map(k -> (BeanDefinition<T>) k).iterator();
        };
    }

    public Iterable<BeanDefinition<?>> definitionsByPrefix(String prefix) {
        return Iterables.concat(beanLookup.keySet().stream().map(k -> definitionsByPrefix(k, prefix)).collect(Collectors.toList()));
    }

    public Iterable<BeanDefinition<?>> definitionsByPrefix(ClassLoader loader, String prefix) {
        final ClassLookup lookup = beanLookup.get(loader);
        final List<String> input = lookup.targetLookup;
        int startPoint = 0;
        int endpoint = input.size() - 1;
        if (input.size() == 0) {
            return () -> Collections.emptyIterator();
        }
        while (true) {
            String stringToTest = input.get((endpoint - startPoint) / 2);
            if (stringToTest.startsWith(prefix)) {
                while (true) {
                    if (!input.get(startPoint).startsWith(prefix)) {
                        startPoint++;
                        break;
                    }
                    if (startPoint == 0) {
                        break;
                    }
                    startPoint--;
                }
                break;
            }
            if (startPoint == endpoint) {
                return () -> Collections.emptyIterator();
            }
            if (stringToTest.compareTo(prefix) > 0) {
                endpoint = (endpoint - startPoint) / 2;
            }
            if (stringToTest.compareTo(prefix) < 0) {
                if (input.get(endpoint).compareTo(prefix) < 0) {
                    return () -> Collections.emptyIterator();
                }
                startPoint = (endpoint - startPoint) / 2;
            }
        }

        while (!input.get(endpoint).startsWith(prefix)) {
            if (endpoint < startPoint) {
                return () -> Collections.emptyIterator();
            }
            endpoint--;
        }

        final int startIndex = startPoint;
        final int endIndex = endpoint;

        return () -> {
            return new Iterator<BeanDefinition<?>>() {
                private int index = startIndex;

                @Override
                public boolean hasNext() {
                    return index <= endIndex;
                }

                @Override
                public BeanDefinition<?> next() {
                    BeanDefinition<?> def = lookup.definitions.get(input.get(index));
                    index++;
                    return def;
                }
            };
        };
    }

    public boolean releaseDefinitions(ClassLoader loader) {
        writeLock.lock();
        if (!beanLookup.containsKey(loader)) {
            writeLock.unlock();
            return false;
        }
        beanLookup.remove(loader);
        writeLock.unlock();
        return true;
    }

    public <T> BeanDefinition<T> getDefinitions(Class<T> beanType) {
        ClassLookup lookup = beanLookup.get(beanType.getClassLoader());
        return (BeanDefinition<T>) lookup.definitions.get(beanType.getName());
    }
}
