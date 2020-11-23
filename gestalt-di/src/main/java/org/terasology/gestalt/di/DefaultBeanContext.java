package org.terasology.gestalt.di;

import org.terasology.context.AbstractBeanDefinition;
import org.terasology.context.Argument;
import org.terasology.context.BeanDefinition;
import org.terasology.context.BeanResolution;
import org.terasology.gestalt.di.instance.BeanProvider;
import org.terasology.gestalt.di.instance.ClassProvider;
import org.terasology.gestalt.di.instance.InjectionUtility;
import org.terasology.gestalt.di.instance.SupplierProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultBeanContext implements AutoCloseable, BeanContext {
    private BeanContext parent;
    private BeanEnvironment environment;
    private final Map<BeanIdentifier, BeanProvider> providers = new HashMap<>();
    private final Map<BeanIdentifier, Object> boundObjects = new ConcurrentHashMap<>();

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
        for (ServiceRegistry.InstanceExpression expression : registry.instanceExpressions) {
            BeanKey key = new BeanKey(expression.root, null);
            if (expression.supplier == null) {
                providers.put(key, new ClassProvider(environment, expression.lifetime, expression.target));
            } else {
                providers.put(key, new SupplierProvider(environment, expression.lifetime, expression.supplier));
            }
        }
    }

    @Override
    public <T> T inject(T instance) {
        BeanDefinition<T> definition = (BeanDefinition<T>) environment.getInstance(instance.getClass());
        if (definition instanceof AbstractBeanDefinition) {
            return ((AbstractBeanDefinition<T>) definition).build(new BeanResolution() {
                @Override
                public <T> T resolveConstructorArgument(Class<T> target, Argument<T> argument) {
                    BeanKey<T> key = InjectionUtility.resolveBeanKey(argument);
                    return inject(key);
                }

                @Override
                public <T> T resolveParameterArgument(Class<T> target, Argument<T> argument) {
                    BeanKey<T> key = InjectionUtility.resolveBeanKey(argument);
                    return inject(key);
                }
            });
        }
        return null;
    }


    @Override
    public <T> T inject(BeanKey<T> identifier) {
        Optional<BeanContext> cntx = Optional.of(this);
        while (cntx.isPresent()) {
            BeanContext beanContext = cntx.get();
            if (beanContext instanceof DefaultBeanContext) {
                DefaultBeanContext defContext = ((DefaultBeanContext) beanContext);
                T target = defContext.internalResolve(identifier, this);
                if (target != null) {
                    return target;
                }
            }
            cntx = getParent();
        }
        return null;
    }

    @Override
    public <T> T fetch(Class<T> clazz) {
        BeanKey key = new BeanKey<T>(clazz, null);
        return (T) inject(key);
    }

    private <T> T internalResolve(BeanIdentifier identifier, DefaultBeanContext context) {
        if (providers.containsKey(identifier)) {
            BeanProvider providers = this.providers.get(identifier);
            T result;
            switch (providers.getLifetime()) {
                case Transient:
                    return (T) providers.get(identifier, this, context);
                case Singleton:
                    if (this.boundObjects.containsKey(identifier)) {
                        return (T) this.boundObjects.get(identifier);
                    }
                    result = (T) providers.get(identifier, this, context);
                    boundObjects.put(identifier, result);
                    return result;
                case Scoped:
                    if (context.boundObjects.containsKey(identifier)) {
                        return (T) context.boundObjects.get(identifier);
                    }
                    result = (T) providers.get(identifier, this, context);
                    context.boundObjects.put(identifier, result);
                    return result;
            }
        }
        return null;
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
                ((AutoCloseable) o).close();
            }
        }
    }

    public Optional<BeanContext> getParent() {
        return Optional.ofNullable(parent);
    }

}
