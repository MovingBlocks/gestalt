package org.terasology.gestalt.di;

import org.terasology.context.BeanDefinition;

import javax.swing.text.html.Option;
import javax.xml.ws.Service;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultBeanContext implements AutoCloseable, BeanContext {
    private BeanContext root;
    private BeanEnvironment environment;
    private final Map<BeanIdentifier, Object> instance = new ConcurrentHashMap<>();
    private final ServiceGraph serviceGraph;

    public DefaultBeanContext(BeanContext root, ServiceRegistry ... registries) {
        this(root, new BeanEnvironment(), registries);
    }

    public DefaultBeanContext(ServiceRegistry ... registries) {
        this(null, new BeanEnvironment(), registries);
    }

    public DefaultBeanContext(BeanContext root, BeanEnvironment environment, ServiceRegistry ... registries) {
        this.root = root;
        this.environment = environment;
        this.serviceGraph = new ServiceGraph(registries);
    }

    public <T> T inject(T instance) {
        //TODO: setup inject
        return null;
    }

    private <T> T internalInject(Class<T> type) {
        BeanDefinition<T> instance =  environment.getInstance(type);
        BeanKey<T> key = new BeanKey<T>(instance.targetClass());

        Optional<BeanContext> cntx = Optional.of(this);
        while (cntx.isPresent()) {
            BeanContext beanContext = cntx.get();
            if(beanContext instanceof  DefaultBeanContext) {
                if(((DefaultBeanContext) beanContext).instance.containsKey(key)) {
                   return (T) ((DefaultBeanContext) beanContext).instance.get(key);
                }
                Optional<T> target =  ((DefaultBeanContext) beanContext).serviceGraph.resolve(instance, beanContext);
                if(target.isPresent()) {
                    ((DefaultBeanContext) beanContext).instance.put(key,target.get());
                    return target.get();
                }
            }
            cntx = getRoot();
        }
        return null;
    }


    public void Configure() {

    }

    public BeanContext GetNestedContainer() {
        return new DefaultBeanContext(this, environment);
    }

    private void readBeanDefinitions() {

    }

    @Override
    public void close() throws Exception {

    }


    public Optional<BeanContext> getRoot() {
        return Optional.ofNullable(root);
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
