package org.terasology.gestalt.di;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultBeanContext implements AutoCloseable, BeanContext {
    private BeanContext parent;
    private BeanEnvironment environment;
    private final Map<BeanIdentifier, Object> instance = new ConcurrentHashMap<>();
    private final ServiceGraph serviceGraph;

    public DefaultBeanContext(BeanContext root, ServiceRegistry ... registries) {
        this(root, new BeanEnvironment(), registries);
    }

    public DefaultBeanContext(ServiceRegistry ... registries) {
        this(null, new BeanEnvironment(), registries);
    }

    public DefaultBeanContext(BeanContext parent, BeanEnvironment environment, ServiceRegistry ... registries) {
        this.parent = parent;
        this.environment = environment;
        this.serviceGraph = new ServiceGraph(environment, this, registries);
    }

    public <T> T inject(T instance) {

        return null;
    }

    private <T> T internalInject(BeanKey<T> beanKey) {
        Optional<BeanContext> cntx = Optional.of(this);
        while (cntx.isPresent()) {
            BeanContext beanContext = cntx.get();
            if(beanContext instanceof  DefaultBeanContext) {
                Optional<T> target =  ((DefaultBeanContext) beanContext).serviceGraph.resolve(beanKey, beanContext);
                if(target.isPresent()) {
                    return target.get();
                }
            }
            cntx = getParent();
        }
        return null;
    }

    @Override
    public <T> boolean contains(BeanIdentifier id){
        return instance.containsKey(id);
    }

    @Override
    public <T> void bind(BeanIdentifier id, T target) {
        instance.put(id, target);
    }

    @Override
    public <T> T get(BeanIdentifier id) {
        return (T) instance.get(id);
    }

    @Override
    public <T> void release(BeanIdentifier identifier) {
        instance.remove(identifier);
    }

    public void Configure() {

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

    @Override
    public <T> T inject(BeanIdentifier definition) {
        return null;
    }
}
