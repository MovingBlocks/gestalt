package org.terasology.gestalt.di;

import org.terasology.gestalt.di.instance.Instance;
import org.terasology.gestalt.di.instance.ObjectInstance;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ServiceGraph {
    private final Map<BeanIdentifier, Instance> instances = new HashMap<>();
    private final BeanContext context;
    private final BeanEnvironment environment;

    public ServiceGraph(BeanEnvironment environment,BeanContext context, ServiceRegistry... registries) {
        this.context = context;
        this.environment = environment;
        for (ServiceRegistry registry : registries) {
            this.bindRegistry(registry);
        }
    }

    public BeanContext serviceContext() {
        return context;
    }

    private void bindRegistry(ServiceRegistry registry) {
        for(ServiceRegistry.InstanceExpression expression: registry.instanceExpressions) {
            BeanKey key = new BeanKey(expression.root);
            if(expression.supplier == null) {

            } else {
                instances.put(key, new ObjectInstance(environment, expression.lifetime, this, expression.supplier));
            }
        }
    }

    public <T> Optional<T> resolve(BeanIdentifier beanKey, BeanContext current) {
        if(!instances.containsKey(beanKey)) {
            Instance instance = instances.get(beanKey);
            return (Optional<T>) instance.get(beanKey,current);
        }
        return Optional.empty();
    }


}
