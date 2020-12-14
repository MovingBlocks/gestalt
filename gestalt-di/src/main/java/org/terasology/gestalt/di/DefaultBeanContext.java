package org.terasology.gestalt.di;

import org.terasology.context.AbstractBeanDefinition;
import org.terasology.context.Argument;
import org.terasology.context.BeanDefinition;
import org.terasology.context.BeanResolution;
import org.terasology.gestalt.di.instance.BeanProvider;
import org.terasology.gestalt.di.instance.ClassProvider;
import org.terasology.gestalt.di.instance.SupplierProvider;
import org.terasology.gestalt.di.qualifiers.Qualifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultBeanContext implements AutoCloseable, BeanContext {
    private BeanContext parent;
    private BeanEnvironment environment;
    private final Map<BeanIdentifier, BeanProvider> providers = new HashMap<>();
    protected final Map<BeanIdentifier, Object> boundObjects = new ConcurrentHashMap<>();

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
    public <T> T inject(T instance) throws Exception {
        try (BeanTransaction transaction = new BeanTransaction()) {
            T result = this.inject(instance, transaction);
            transaction.commit();
            return result;
        }
    }


    @Override
    public <T> T inject(T instance, BeanTransaction transaction) {
        BeanDefinition<T> definition = (BeanDefinition<T>) environment.getDefinitions(instance.getClass());

        if (definition instanceof AbstractBeanDefinition) {
            return ((AbstractBeanDefinition<T>) definition).build(new BeanResolution() {
                @Override
                public <T> T resolveConstructorArgument(Class<T> target, Argument<T> argument) throws Exception {
                    BeanKey<T> key = BeanKeys.resolveBeanKey(argument.getType(), argument);
                    return getBean(key, transaction);
                }

                @Override
                public <T> T resolveParameterArgument(Class<T> target, Argument<T> argument) throws Exception {
                    BeanKey<T> key = BeanKeys.resolveBeanKey(argument.getType(), argument);
                    return getBean(key, transaction);
                }
            });
        }
        return null;
    }


    @Override
    public <T> T getBean(BeanKey<T> identifier) throws Exception {
        try (BeanTransaction transaction = new BeanTransaction()) {
            T result = getBean(identifier, transaction);
            transaction.commit();
            return result;
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public <T> T getBean(BeanKey<T> identifier, BeanTransaction transaction) throws Exception {
        Optional<BeanContext> cntx = Optional.of(this);
        while (cntx.isPresent()) {
            BeanContext beanContext = cntx.get();
            if (beanContext instanceof DefaultBeanContext) {
                DefaultBeanContext defContext = ((DefaultBeanContext) beanContext);
                T target = defContext.internalResolve(identifier, this, transaction);
                if (target != null) {
                    return target;
                }
            }
            cntx = getParent();
        }
        return null;
    }

    /**
     * @param identifier
     * @param currentContext the context that the object is being resolve to
     * @param <T>
     * @return
     */
    private <T> T internalResolve(BeanIdentifier identifier, DefaultBeanContext currentContext, BeanTransaction transaction) throws Exception {
        if (providers.containsKey(identifier)) {
            BeanProvider provider = this.providers.get(identifier);
            T result;
            switch (provider.getLifetime()) {
                case Transient:
                    return (T) provider.get(identifier, this, currentContext, transaction);
                case Singleton:
                    return (T) transaction.bind(this, identifier, () -> (T) provider.get(identifier, this, currentContext, transaction));
                case Scoped:
                    return (T) transaction.bind(currentContext, identifier, () -> (T) provider.get(identifier, this, currentContext, transaction));
            }
        }
        return null;
    }

    @Override
    public <T> T getBean(Class<T> clazz) throws Exception {
        try (BeanTransaction transaction = new BeanTransaction()) {
            T result = (T) getBean(clazz, transaction);
            transaction.commit();
            return result;
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public <T> T getBean(Class<T> clazz, BeanTransaction transaction) throws Exception {
        BeanKey<T> identifier = new BeanKey(clazz, null);
        return (T) getBean(identifier, transaction);
    }

    @Override
    public <T> T getBean(Class<T> clazz, Qualifier<T> qualifier) throws Exception {
        try (BeanTransaction transaction = new BeanTransaction()) {
            T result = (T) getBean(clazz, qualifier, transaction);
            transaction.commit();
            return result;
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public <T> T getBean(Class<T> clazz, Qualifier<T> qualifier, BeanTransaction transaction) throws Exception {
        BeanKey<T> identifier = new BeanKey(clazz, qualifier);
        return (T) getBean(identifier, transaction);
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
