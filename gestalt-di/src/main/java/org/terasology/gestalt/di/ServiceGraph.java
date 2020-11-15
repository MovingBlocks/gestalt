package org.terasology.gestalt.di;

import org.terasology.context.BeanDefinition;
import org.terasology.gestalt.di.instance.Instance;

import java.util.Map;
import java.util.Optional;

public class ServiceGraph {
    private Map<BeanIdentifier, Instance> instances;
    private BeanContext context;
    public ServiceGraph(BeanContext context) {
        this.context = context;
    }

    public ServiceGraph(ServiceRegistry... registries) {
        for (ServiceRegistry registry : registries) {
            this.bindRegistry(registry);
        }
    }

    private void bindRegistry(ServiceRegistry registry) {
        for(ServiceRegistry.InstanceExpression expression: registry.instanceExpressions) {
            switch (expression.lifetime) {
                case Scoped:
                    break;
                case Singleton:
                    break;
                case Transient:
                    break;
            }
        }
    }

    public <T> Optional<T> resolve(BeanDefinition<T> beanKey, BeanContext from) {

        return Optional.empty();
    }


}
