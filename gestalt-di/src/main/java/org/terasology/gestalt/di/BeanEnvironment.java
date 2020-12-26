package org.terasology.gestalt.di;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import org.terasology.context.BeanDefinition;
import org.terasology.context.SoftServiceLoader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class BeanEnvironment {

    private static class ClassRef<T> {
        public final Class<T> target;
        public final String prefix;
        public final BeanDefinition<T> definition;

        public ClassRef(BeanDefinition<T> definition) {
            this.target = definition.targetClass();
            this.prefix = target.getName();
            this.definition = definition;
        }

        public ClassRef(Class<T> target) {
            this.target = target;
            this.prefix = target.getName();
            this.definition = null;
        }

        public ClassRef(String prefix) {
            target = null;
            this.definition = null;
            this.prefix = prefix;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ClassRef<?> that = (ClassRef<?>) o;
            return Objects.equals(target, that.target);
        }

        @Override
        public int hashCode() {
            return Objects.hash(target);
        }
    }

    private static class ClassLookup {
        private ClassRef<?>[] namespaceIndex;
        private Map<Class<?>, ClassRef<?>[]> interfaceIndex;
        private Map<Class<?>, BeanDefinition<?>> definitions;
    }

    public static final ClassLoader BaseClassLoader = BeanEnvironment.class.getClassLoader();
    private final Map<ClassLoader, ClassLookup> beanLookup = new HashMap<>();
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();

    public BeanEnvironment() {
        loadDefinitions(BeanEnvironment.BaseClassLoader);
    }

    public void loadDefinitions(ClassLoader loader) {
        if (beanLookup.containsKey(loader)) {
            return;
        }
        writeLock.lock();
        try {
            beanLookup.computeIfAbsent(loader, (ld) -> {
                SoftServiceLoader<BeanDefinition> definitions = new SoftServiceLoader<>(BeanDefinition.class, ld);
                ClassLookup lookup = new ClassLookup();

                List<ClassRef<?>> namespaceIndex = new ArrayList<>();
                Map<Class<?>, BeanDefinition<?>> definitionLookup = Maps.newHashMap();
                Multimap<Class<?>, ClassRef> interfaceIndex = HashMultimap.create();
                for (BeanDefinition<?> definition : definitions) {
                    // exclude objects that are from a different classloader
                    if (definition.targetClass().getClassLoader() != loader) {
                        continue;
                    }
                    namespaceIndex.add(new ClassRef<>(definition));
                    definitionLookup.put(definition.targetClass(), definition);
                    for (Class<?> clazzInterface : definition.targetClass().getInterfaces()) {
                        interfaceIndex.put(clazzInterface, new ClassRef<>(definition));
                    }
                }
                namespaceIndex.sort((Comparator<ClassRef>) (a1, a2) -> a1.prefix.compareTo(a2.prefix));
                lookup.namespaceIndex = namespaceIndex.toArray(new ClassRef[0]);
                lookup.interfaceIndex = interfaceIndex.asMap().entrySet().stream().collect(
                    Collectors.toMap(k -> k.getKey(), v -> {
                        ClassRef[] result = v.getValue().toArray(new ClassRef[0]);
                        Arrays.sort(result, (classRef, t1) -> classRef.prefix.compareTo(t1.prefix));
                        return result;
                    })
                );
                lookup.definitions = definitionLookup;

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
        return lookup.definitions.containsKey(clazz);
    }

    private Optional<Range<Integer>> findPrefixBounds(String prefix, ClassRef<?>[] input) {
        if (input.length == 0) {
            return Optional.empty();
        }

        if (Strings.isNullOrEmpty(prefix)) {
            return Optional.of(Range.closed(0, input.length - 1));
        }

        int startPoint = 0;
        int endpoint = input.length - 1;
        while (true) {
            String stringToTest = input[startPoint + (endpoint - startPoint) / 2].prefix;
            if (stringToTest.startsWith(prefix)) {
                break;
            }
            if (startPoint == endpoint) {
                return Optional.empty();
            }
            if (stringToTest.compareTo(prefix) > 0) {
                endpoint = startPoint + ((endpoint - startPoint) / 2);
            }
            if (stringToTest.compareTo(prefix) < 0) {
                if (input[endpoint].prefix.compareTo(prefix) < 0) {
                    return Optional.empty();
                }
                startPoint = startPoint + ((endpoint - startPoint) / 2);
            }
        }

        while (!input[endpoint].prefix.startsWith(prefix)) {
            if (endpoint < startPoint) {
                return Optional.empty();
            }
            endpoint--;
        }

        while (!input[startPoint].prefix.startsWith(prefix)) {
            if (endpoint < startPoint) {
                return Optional.empty();
            }
            startPoint++;
        }

        return Optional.of(Range.closed(startPoint, endpoint));
    }

    public <T> Iterable<BeanDefinition<?  extends T>> byInterface(ClassLoader loader, Class<T> targetInterface) {
        final ClassLookup lookup = beanLookup.get(loader);
        if (!lookup.interfaceIndex.containsKey(targetInterface)) {
            return Collections::emptyIterator;
        }
        return Arrays.stream(lookup.interfaceIndex.get(targetInterface)).map(k -> (BeanDefinition<? extends T>) k.definition).collect(Collectors.toList());
    }

    public <T> Iterable<BeanDefinition<? extends T>> byInterface(Class<T> targetInterface) {
        return Iterables.concat(beanLookup.keySet().stream().map(k -> byInterface(k, targetInterface)).collect(Collectors.toList()));
    }

    public <T> Iterable<BeanDefinition<?>> byPrefixAndInterface(ClassLoader loader, String prefix, Class<T> targetInterface) {
        final ClassLookup lookup = beanLookup.get(loader);
        ClassRef<?>[] targetClasses = lookup.interfaceIndex.get(targetInterface);
        Optional<Range<Integer>> range = findPrefixBounds(prefix, targetClasses);
        if (range.isPresent()) {
            Range<Integer> itr = range.get();
            return () -> new Iterator<BeanDefinition<?>>() {
                private int index = itr.lowerEndpoint();

                @Override
                public boolean hasNext() {
                    return index <= itr.upperEndpoint();
                }

                @Override
                public BeanDefinition<?> next() {
                    BeanDefinition<?> def = targetClasses[index].definition;
                    index++;
                    return def;
                }
            };
        }
        return Collections::emptyIterator;
    }

    public Iterable<BeanDefinition<?>> byPrefix(String prefix) {
        return Iterables.concat(beanLookup.keySet().stream().map(k -> byPrefix(k, prefix)).collect(Collectors.toList()));
    }

    public <T> Iterable<BeanDefinition<?>> byPrefix(ClassLoader loader, String prefix) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(prefix));
        final ClassLookup lookup = beanLookup.get(loader);

        Optional<Range<Integer>> range = findPrefixBounds(prefix, lookup.namespaceIndex);
        if (range.isPresent()) {
            Range<Integer> itr = range.get();
            return () -> new Iterator<BeanDefinition<?>>() {
                private int index = itr.lowerEndpoint();

                @Override
                public boolean hasNext() {
                    return index <= itr.upperEndpoint();
                }

                @Override
                public BeanDefinition<?> next() {
                    BeanDefinition<?> def = lookup.namespaceIndex[index].definition;
                    index++;
                    return def;
                }
            };
        }
        return Collections::emptyIterator;
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

    public Iterable<ClassLoader> classLoaders() {
        return beanLookup.keySet();
    }

    public <T> Optional<BeanDefinition<?>> getDefinition(Class<T> beanType) {
        final ClassLookup lookup = beanLookup.get(beanType.getClassLoader());
        return Optional.ofNullable(lookup.definitions.get(beanType));
    }
}
