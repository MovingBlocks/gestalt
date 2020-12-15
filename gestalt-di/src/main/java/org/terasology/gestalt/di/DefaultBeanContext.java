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

    @Override
    public <T> Optional<T> inject(T instance){
        try (BeanTransaction transaction = new BeanTransaction()) {
            Optional<T> result = this.inject(instance, transaction);
            transaction.commit();
            return result;
        }
    }


    @Override
    public <T> Optional<T> inject(T instance, BeanTransaction transaction) {
        BeanDefinition<T> definition = (BeanDefinition<T>) environment.getDefinitions(instance.getClass());

        if (definition instanceof AbstractBeanDefinition) {
            return definition.build(new BeanResolution() {
                @Override
                public <T> Optional<T> resolveConstructorArgument(Class<T> target, Argument<T> argument) {
                    BeanKey<T> key = BeanKeys.resolveBeanKey(argument.getType(), argument);
                    return getBean(key, transaction);
                }

                @Override
                public <T> Optional<T> resolveParameterArgument(Class<T> target, Argument<T> argument) {
                    BeanKey<T> key = BeanKeys.resolveBeanKey(argument.getType(), argument);
                    return getBean(key, transaction);
                }
            });
        }
        return Optional.empty();
    }


    @Override
    public <T> Optional<T> getBean(BeanKey<T> identifier) {
        try (BeanTransaction transaction = new BeanTransaction()) {
            Optional<T> result = getBean(identifier, transaction);
            transaction.commit();
            return result;
        } catch (DependencyInjectionException e) {
            throw e;
        }
    }

    @Override
    public <T> Optional<T> getBean(BeanKey<T> identifier, BeanTransaction transaction) {
        Optional<BeanContext> cntx = Optional.of(this);
        while (cntx.isPresent()) {
            BeanContext beanContext = cntx.get();
            if (beanContext instanceof DefaultBeanContext) {
                DefaultBeanContext defContext = ((DefaultBeanContext) beanContext);
                Optional<T> target = defContext.internalResolve(identifier, this, transaction);
                if(target.isPresent()){
                    return target;
                }
            }
            cntx = getParent();
        }
        return Optional.empty();
    }

    /**
     * @param identifier
     * @param currentContext the context that the object is being resolve to
     * @param <T>
     * @return
     */
    private <T> Optional<T> internalResolve(BeanIdentifier identifier, DefaultBeanContext currentContext, BeanTransaction transaction) {
        if (providers.containsKey(identifier)) {
            BeanProvider<T> provider = (BeanProvider<T>) this.providers.get(identifier);
            switch (provider.getLifetime()) {
                case Transient:
                    return provider.get(identifier, this, currentContext, transaction);
                case Singleton:
                    return transaction.bind(this, identifier, () ->  provider.get(identifier, this, currentContext, transaction));
                case Scoped:
                    return transaction.bind(currentContext, identifier, () -> provider.get(identifier, this, currentContext, transaction));
            }
        }
        return Optional.empty();
    }

    @Override
    public <T> Optional<T> getBean(Class<T> clazz) {
        try (BeanTransaction transaction = new BeanTransaction()) {
            Optional<T> result = getBean(clazz, transaction);
            transaction.commit();
            return result;
        } catch (DependencyInjectionException e) {
            throw e;
        }
    }

    @Override
    public <T> Optional<T> getBean(Class<T> clazz, BeanTransaction transaction) {
        BeanKey<T> identifier = new BeanKey<>(clazz, null);
        return getBean(identifier, transaction);
    }

    @Override
    public <T> Optional<T> getBean(Class<T> clazz, Qualifier<T> qualifier) {
        try (BeanTransaction transaction = new BeanTransaction()) {
            Optional<T> result = getBean(clazz, qualifier, transaction);
            transaction.commit();
            return result;
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public <T> Optional<T> getBean(Class<T> clazz, Qualifier<T> qualifier, BeanTransaction transaction) {
        BeanKey<T> identifier = new BeanKey<>(clazz, qualifier);
        return getBean(identifier, transaction);
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
