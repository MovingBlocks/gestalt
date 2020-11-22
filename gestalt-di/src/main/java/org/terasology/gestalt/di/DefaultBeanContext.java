package org.terasology.gestalt.di;

import org.terasology.gestalt.di.instance.BeanProvider;
import org.terasology.gestalt.di.instance.ClassProvider;
import org.terasology.gestalt.di.instance.SupplierProvider;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultBeanContext implements AutoCloseable, BeanContext {
    private BeanContext parent;
    private BeanEnvironment environment;
    private final Map<BeanIdentifier, BeanProvider> providers = new HashMap<>();
    private final Map<BeanIdentifier, Object> boundObjects = new ConcurrentHashMap<>();

    public DefaultBeanContext(BeanContext root, ServiceRegistry ... registries) {
        this(root, new BeanEnvironment(), registries);
    }

    public DefaultBeanContext(ServiceRegistry ... registries) {
        this(null, new BeanEnvironment(), registries);
    }

    public DefaultBeanContext(BeanContext parent, BeanEnvironment environment, ServiceRegistry ... registries) {
        this.parent = parent;
        this.environment = environment;
        for (ServiceRegistry registry : registries) {
            this.bindRegistry(registry);
        }
    }

    private void  bindRegistry(ServiceRegistry registry) {
        for (ServiceRegistry.InstanceExpression expression : registry.instanceExpressions) {
            BeanKey key = new BeanKey(expression.root);
            if (expression.supplier == null) {
                providers.put(key, new ClassProvider(environment, expression.lifetime, this, expression.target));
            } else {
                providers.put(key, new SupplierProvider(environment, expression.lifetime, this, expression.supplier));
            }
        }
    }

    public <T> T inject(T instance) {

        return null;
    }

    @Override
    public <T> T inject(BeanKey<T> identifier) {
        Optional<BeanContext> cntx = Optional.of(this);
        while (cntx.isPresent()) {
            BeanContext beanContext = cntx.get();
            if(beanContext instanceof  DefaultBeanContext) {
                DefaultBeanContext defContext = ((DefaultBeanContext) beanContext);
                T target = defContext.internalResolve(identifier, defContext);
                if(target != null){
                    return target;
                }
            }
            cntx = getParent();
        }
        return null;
    }

    private <T> T internalResolve(BeanIdentifier identifier, DefaultBeanContext context) {
        if (providers.containsKey(identifier)) {
            BeanProvider providers = this.providers.get(identifier);
            T result;
            switch (providers.getLifetime()) {
                case Transient:
                    return (T) providers.get(identifier, context);
                case Singleton:
                    if(this.boundObjects.containsKey(identifier)) {
                        return (T) this.boundObjects.get(identifier);
                    }
                    result = (T) providers.get(identifier, context);
                    boundObjects.put(identifier,result);
                    return result;
                case Scoped:
                    if(context.boundObjects.containsKey(identifier)) {
                        return (T) context.boundObjects.get(identifier);
                    }
                    result = (T) providers.get(identifier, context);
                    context.boundObjects.put(identifier,result);
                    return result;
            }
        }
        return null;
    }

    public BeanContext getNestedContainer() {
        return new DefaultBeanContext(this, environment);
    }

    public BeanContext getNestedContainer(ServiceRegistry ... registries) {
        return new DefaultBeanContext(this, environment, registries);
    }


    private void readBeanDefinitions() {

    }

    @Override
    public void close() throws Exception {

    }


    public Optional<BeanContext> getParent() {
        return Optional.ofNullable(parent);
    }

    @Override
    public <T> T inject(Annotation parent, T instance) {
        return null;
    }

}
