package org.terasology.gestalt.di;

import org.terasology.context.AbstractBeanDefinition;
import org.terasology.context.Argument;
import org.terasology.context.BeanDefinition;
import org.terasology.context.BeanResolution;
import org.terasology.context.exception.DependencyInjectionException;
import org.terasology.gestalt.di.instance.BeanProvider;
import org.terasology.gestalt.di.instance.ClassProvider;
import org.terasology.gestalt.di.instance.SupplierProvider;
import org.terasology.gestalt.di.qualifiers.Qualifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class DefaultBeanContext implements AutoCloseable, BeanContext {
    protected final Map<BeanIdentifier, Object> boundObjects = new ConcurrentHashMap<>();
    private final Map<BeanIdentifier, BeanProvider<?>> providers = new HashMap<>();
    private final BeanContext parent;
    private final BeanEnvironment environment;

    public DefaultBeanContext(BeanContext root, ServiceRegistry... registries) {
        this(root, new BeanEnvironment(), registries);
    }

    public DefaultBeanContext(ServiceRegistry... registries) {
        this(null, new BeanEnvironment(), registries);
    }

    public DefaultBeanContext(BeanContext parent, BeanEnvironment environment, ServiceRegistry... registries) {
        this.parent = parent;
        this.environment = environment;
        for (ServiceRegistry registry : registries) {
            this.bindRegistry(registry);
        }
    }

    private void bindRegistry(ServiceRegistry registry) {
        for (ServiceRegistry.InstanceExpression<?> expression : registry.instanceExpressions) {
            BeanKey<?> key = new BeanKey<>(expression.root, null);
            if (expression.supplier == null) {
                providers.put(key, new ClassProvider(environment, expression.lifetime, expression.target));
            } else {
                providers.put(key, new SupplierProvider(environment, expression.lifetime, expression.supplier));
            }
        }
    }

    static <T> Optional<T> bindBean(DefaultBeanContext context, BeanIdentifier identifier, Supplier<Optional<T>> supplier) {
        if (context.boundObjects.containsKey(identifier)) {
            return Optional.of((T) context.boundObjects.get(identifier));
        }
        Optional<T> result = supplier.get();
        result.ifPresent(t -> context.boundObjects.put(identifier, t));
        return result;
    }


    @Override
    public <T> Optional<T> inject(T instance) {
        BeanDefinition<T> definition = (BeanDefinition<T>) environment.getDefinitions(instance.getClass());

        if (definition instanceof AbstractBeanDefinition) {
            return definition.build(new BeanResolution() {
                @Override
                public <T> Optional<T> resolveConstructorArgument(Class<T> target, Argument<T> argument) {
                    BeanKey<T> key = BeanKeys.resolveBeanKey(argument.getType(), argument);
                    return getBean(key);
                }

                @Override
                public <T> Optional<T> resolveParameterArgument(Class<T> target, Argument<T> argument) {
                    BeanKey<T> key = BeanKeys.resolveBeanKey(argument.getType(), argument);
                    return getBean(key);
                }
            });
        }
        return Optional.empty();
    }



    @Override
    public <T> Optional<T> getBean(BeanKey<T> identifier) {
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
            cntx = getParent();
        }
        return Optional.empty();
    }

    /**
     * @param identifier
     * @param targetContext the context that the object is being resolve to
     * @param <T>
     * @return
     */
    private <T> Optional<T> internalResolve(BeanIdentifier identifier, DefaultBeanContext targetContext) {
        if (providers.containsKey(identifier)) {
            BeanProvider<T> provider = (BeanProvider<T>) this.providers.get(identifier);
            switch (provider.getLifetime()) {
                case Transient:
                    return provider.get(identifier, this, targetContext);
                case Singleton:
                    return DefaultBeanContext.bindBean(this, identifier, () -> provider.get(identifier, this, targetContext));
                case Scoped:
                    return DefaultBeanContext.bindBean(targetContext, identifier, () -> provider.get(identifier, this, targetContext));
            }
        }
        return Optional.empty();
    }

    @Override
    public <T> Optional<T> getBean(Class<T> clazz) {
        BeanKey<T> identifier = new BeanKey<>(clazz, null);
        return getBean(identifier);
    }

    @Override
    public <T> Optional<T> getBean(Class<T> clazz, Qualifier<T> qualifier) {
        BeanKey<T> identifier = new BeanKey<>(clazz, qualifier);
        return getBean(identifier);
    }

    public BeanContext getNestedContainer() {
        return new DefaultBeanContext(this, environment);
    }

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
