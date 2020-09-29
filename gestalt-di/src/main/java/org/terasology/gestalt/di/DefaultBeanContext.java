package org.terasology.gestalt.di;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultBeanContext implements AutoCloseable, BeanContext {
    private BeanContext root;
    private ServiceGraph serviceGraph;
    private Map<BeanIdentifier, Object> instance = new ConcurrentHashMap<>();


    public DefaultBeanContext(ServiceRegistry registry) {
    }

    public DefaultBeanContext(BeanContext root) {
        this(root, new ServiceGraph());
    }

    public DefaultBeanContext(BeanContext root, ServiceGraph serviceGraph) {

        this.root = root;
        this.serviceGraph = serviceGraph;
    }

    public <T> T inject(T instance) {
        //TODO: setup inject
        return null;
    }

    private <T> T internalInject(Class<T> type) {

        return null;
    }


    public void Configure() {

    }

    public BeanContext GetNestedContainer() {
        return new DefaultBeanContext(this, new ServiceGraph());
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
