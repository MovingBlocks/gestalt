package org.terasology.gestalt.di;

import org.terasology.context.BeanDefinition;
import org.terasology.gestalt.di.instance.Instance;
import org.terasology.gestalt.di.instance.ObjectInstance;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ServiceGraph {
    private final Map<BeanIdentifier, Instance> instances = new HashMap<>();
    private final BeanContext context;

    public ServiceGraph(BeanContext context, ServiceRegistry... registries) {
        this.context = context;
        for (ServiceRegistry registry : registries) {
            this.bindRegistry(registry);
        }
    }

    private void bindRegistry(ServiceRegistry registry) {
        for(ServiceRegistry.InstanceExpression expression: registry.instanceExpressions) {
            BeanKey key = new BeanKey(expression.root);
            if(expression.supplier == null) {

            } else {
                instances.put(key, new ObjectInstance(expression.lifetime, expression.supplier, context));
            }
        }
    }

    public <T> Optional<T> resolve(BeanDefinition<T> beanKey, BeanContext current) {
        if(!instances.containsKey(beanKey)) {
            Instance instance = instances.get(beanKey);
            return (Optional<T>) instance.get(beanKey,current);
        }
        return Optional.empty();
    }


}
