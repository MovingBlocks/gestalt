package org.terasology.gestalt.di;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.terasology.context.AbstractBeanDefinition;
import org.terasology.context.BeanDefinition;
import org.terasology.context.EmptyAnnotationMetadata;
import org.terasology.gestalt.di.exceptions.BeanResolutionException;
import org.terasology.gestalt.di.injection.Qualifier;
import org.terasology.gestalt.di.instance.BeanProvider;
import org.terasology.gestalt.di.instance.ClassProvider;
import org.terasology.gestalt.di.instance.SupplierProvider;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DefaultBeanContext implements AutoCloseable, BeanContext {
    protected final Map<BeanKey, Object> boundObjects = new ConcurrentHashMap<>();
    protected final Map<BeanKey, BeanProvider<?>> providers = new ConcurrentHashMap<>();
    protected final Map<Qualifier, BeanIntercept> beanInterceptMapping = new ConcurrentHashMap<>();
    private final Multimap<Qualifier, BeanKey> qualifierMapping = HashMultimap.create();
    private final Multimap<Class, BeanKey> interfaceMapping = HashMultimap.create();

    private final BeanContext parent;
    private final BeanEnvironment environment;

    public DefaultBeanContext(BeanContext root, ServiceRegistry... registries) {
        this(root, new BeanEnvironment(), registries);
    }

    public DefaultBeanContext(ServiceRegistry... registries) {
        this(null, new BeanEnvironment(), registries);
    }

    public DefaultBeanContext(BeanContext parent, BeanEnvironment environment, ServiceRegistry... registries) {
        Preconditions.checkArgument(parent != this, "bean context can't reference itself");
        this.parent = parent;
        this.environment = environment;
        for (ServiceRegistry registry : registries) {
            this.bindRegistry(registry);
        }
    }

    static <T> Optional<T> bindBean(DefaultBeanContext context, BeanKey identifier, Supplier<Optional<T>> supplier) {
        if (context.boundObjects.containsKey(identifier)) {
            return Optional.of((T) context.boundObjects.get(identifier));
        }
        Optional<T> result = supplier.get();
        result.ifPresent(t -> context.boundObjects.put(identifier, t));
        return result;
    }

    private void bindRegistry(ServiceRegistry registry) {
        for (ClassLoader loader : registry.classLoaders) {
            this.environment.loadDefinitions(loader);
        }
        for (BeanScanner scanner : registry.scanners) {
            scanner.apply(registry, environment);
        }
        for (ServiceRegistry.InstanceExpression<?> expression : registry.instanceExpressions) {
            bindExpression(expression);
        }
        this.beanInterceptMapping.putAll(registry.intercepts);

        // register self as a singleton instance that is scoped to current context
        bindExpression(new ServiceRegistry.InstanceExpression<>(BeanContext.class).lifetime(Lifetime.Singleton).use(() -> this));
    }

    private <T> void bindExpression(ServiceRegistry.InstanceExpression<T> expression) {
        BeanKey<?> key = new BeanKey(expression.target)
                .use(expression.root)
                .qualifiedBy(expression.qualifier);
        if (expression.target == expression.root) {
            for (Class impl : expression.target.getInterfaces()) {
                interfaceMapping.put(impl, key);
            }
        } else {
            interfaceMapping.put(expression.root, key);
        }
        if (expression.qualifier != null) {
            qualifierMapping.put(expression.qualifier, key);
        }

        if (expression.supplier == null) {
            providers.put(key, new ClassProvider(environment, expression.lifetime, expression.target));
        } else {
            providers.put(key, new SupplierProvider(environment, expression.lifetime, expression.target, expression.supplier));
        }
    }

    @Override
    public <T> Optional<T> inject(T instance) {
        BeanDefinition<T> definition = (BeanDefinition<T>) environment.getDefinition(instance.getClass());
        if (definition instanceof AbstractBeanDefinition) {
            return definition.inject(instance, new DefaultBeanResolution(this, environment));
        }
        return Optional.empty();
    }

    @Override
    public <T> T getBean(BeanKey<T> identifier) {
        Optional<T> result = findBean(identifier);
        return result.orElseThrow(() -> new BeanResolutionException(identifier));
    }

    @Override
    public <T> Optional<T> findBean(BeanKey<T> identifier) {
        Optional<BeanContext> cntx = Optional.of(this);
        while (cntx.isPresent()) {
            BeanContext beanContext = cntx.get();
            if (beanContext instanceof DefaultBeanContext) {
                DefaultBeanContext defContext = ((DefaultBeanContext) beanContext);
                Optional<T> target = defContext.internalResolve(identifier, this);
                if (target.isPresent()) {
                    return target;
                }
            }
            cntx = cntx.get().getParent();
        }
        return Optional.empty();
    }

    private Optional<BeanKey> findConcreteBeanKey(BeanKey identifier) {
        List<BeanKey> beanKeys = getBeanKeys(identifier).collect(Collectors.toList());
        if(beanKeys.size() > 1){
            throw new BeanResolutionException(beanKeys);
        }
        return beanKeys.stream().findFirst();
    }

    private Stream<BeanKey> getBeanKeys(BeanKey identifier) {
        Collection<BeanKey> result = null;
        if (providers.containsKey(identifier)) {
            return Stream.of(identifier);
        }

        if (identifier.qualifier != null) {
            result = Sets.newHashSet(qualifierMapping.get(identifier.qualifier));
        }

        if (identifier.baseType.isInterface()) {
            if (result != null) {
                Collection<BeanKey> implementing = interfaceMapping.get(identifier.baseType);
                if (implementing != null) {
                    result.retainAll(implementing);
                }
            } else {
                result = Sets.newHashSet(interfaceMapping.get(identifier.baseType));
            }
        } else if (identifier.baseType == identifier.implementingType) {
            Collection<BeanKey> implementing = interfaceMapping.get(identifier.baseType);
            for (Class implType : identifier.baseType.getInterfaces()) {
                Collection<BeanKey> temp = interfaceMapping.get(implType);
                if (temp == null || temp.size() == 0) {
                    continue;
                }
                implementing.addAll(temp.stream().filter(k -> k.baseType == identifier.baseType).collect(Collectors.toSet()));
            }
            if (result != null && implementing != null) {
                result.retainAll(implementing);
            } else if (implementing != null) {
                result = implementing;
            }
        } else {
            Collection<BeanKey> implementing = interfaceMapping.get(identifier.implementingType);
            if (result != null && implementing != null) {
                result.retainAll(implementing);
            } else if (implementing != null) {
                result = implementing.stream().filter(k -> k.baseType == identifier.baseType).collect(Collectors.toSet());
            }
        }
        if (result == null) {
            return Stream.of();
        }
        return result.stream();
    }

    /**
     * @param identifier
     * @param targetContext the context that the object is being resolve to
     * @param <T>
     * @return
     */
    private <T> Optional<T> internalResolve(BeanKey identifier, DefaultBeanContext targetContext) {
        Optional<BeanKey> key = findConcreteBeanKey(identifier);

        if(identifier.annotation !=  EmptyAnnotationMetadata.EMPTY_ARGUMENT) {
            BeanIntercept intercept = targetContext.beanInterceptMapping.get(identifier.qualifier);
            if (intercept != null) {
                Optional<T> result = intercept.single(identifier, identifier.annotation);
                if(result.isPresent()) {
                    return result;
                }
            }
        }

        if (key.isPresent()) {
            BeanProvider<T> provider = (BeanProvider<T>) providers.get(key.get());
            switch (provider.getLifetime()) {
                case Transient:
                    return provider.get(key.get(), this, targetContext);
                case Singleton:
                    return DefaultBeanContext.bindBean(this, key.get(), () -> provider.get(key.get(), this, targetContext));
                case Scoped:
                case ScopedToChildren:
                    if (provider.getLifetime() == Lifetime.ScopedToChildren && targetContext == this) {
                        return Optional.empty();
                    }
                    return DefaultBeanContext.bindBean(targetContext, key.get(), () -> provider.get(key.get(), this, targetContext));
            }
        }

        return Optional.empty();
    }

    @Override
    public <T> T getBean(Class<T> clazz) {
        BeanKey<T> identifier = new BeanKey<>(clazz);
        return getBean(identifier);
    }

    @Override
    public <T> Optional<T> findBean(Class<T> clazz) {
        BeanKey<T> identifier = new BeanKey<>(clazz);
        return findBean(identifier);
    }

    @Override
    public <T> T getBean(Class<T> clazz, Qualifier qualifier) {
        BeanKey<T> identifier = new BeanKey<>(clazz)
            .qualifiedBy(qualifier);
        return getBean(identifier);
    }

    @Override
    public <T> List<T> getBeans(BeanKey<T> identifier) {
        Optional<BeanContext> cntx = Optional.of(this);
        Stream<T> all = Stream.of();
        while (cntx.isPresent()) {
            BeanContext beanContext = cntx.get();
            if (beanContext instanceof DefaultBeanContext) {
                DefaultBeanContext defContext = ((DefaultBeanContext) beanContext);
                Stream<T> target = defContext.internalMultipleResolve(identifier, this);
                all = Stream.concat(all, target);
            }
            cntx = cntx.get().getParent();
        }
        return all.collect(Collectors.toList());
    }

    @Override
    public <T> List<T> getBeans(Class<T> clazz) {
        BeanKey<T> identifier = new BeanKey<>(clazz);
        return getBeans(identifier);
    }

    @Override
    public <T> List<T> getBeans(Class<T> clazz, Qualifier qualifier) {
        BeanKey<T> identifier = new BeanKey<>(clazz)
                .qualifiedBy(qualifier);
        return getBeans(identifier);
    }

    private <T> Stream<T> internalMultipleResolve(BeanKey identifier, DefaultBeanContext targetContext) {
        return getBeanKeys(identifier)
                .map(key -> {
                    BeanIntercept intercept = targetContext.beanInterceptMapping.get(identifier);
                    if (intercept != null) {
                        Optional<T> result = intercept.single(identifier, identifier.annotation);
                        if(result.isPresent()) {
                            return result;
                        }
                    }

                    BeanProvider<T> provider = (BeanProvider<T>) providers.get(key);
                    switch (provider.getLifetime()) {
                        case Transient:
                            return provider.get(key, this, targetContext);
                        case Singleton:
                            return DefaultBeanContext.bindBean(this, key, () -> provider.get(key, this, targetContext));
                        case Scoped:
                        case ScopedToChildren:
                            if (provider.getLifetime() == Lifetime.ScopedToChildren && targetContext == this) {
                                return Optional.empty();
                            }
                            return DefaultBeanContext.bindBean(targetContext, key, () -> provider.get(key, this, targetContext));
                    }
                    return Optional.empty();
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(b -> (T) b);
    }

    @Override
    public <T> Optional<T> findBean(Class<T> clazz, Qualifier qualifier) {
        BeanKey<T> identifier = new BeanKey<>(clazz)
                .qualifiedBy(qualifier);
        return findBean(identifier);
    }


    @Override
    public BeanContext getNestedContainer() {
        return new DefaultBeanContext(this, environment);
    }

    @Override
    public BeanContext getNestedContainer(ServiceRegistry... registries) {
        return new DefaultBeanContext(this, environment, registries);
    }

    @Override
    public void close() throws Exception {
        for (Object o : this.boundObjects.values()) {
            if (o instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) o).close();
                } catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                }
            }
        }
    }

    public Optional<BeanContext> getParent() {
        return Optional.ofNullable(parent);
    }
}
