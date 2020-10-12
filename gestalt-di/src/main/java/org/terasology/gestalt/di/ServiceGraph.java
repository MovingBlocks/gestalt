package org.terasology.gestalt.di;

import org.terasology.gestalt.di.instance.Instance;

import java.util.Collection;
import java.util.Map;
import java.util.Stack;
import java.util.function.Supplier;

public class ServiceGraph {
    private Map<BeanIdentifier, Instance> instances;
    private Stack<Instance> path = new Stack<>();

    public ServiceGraph(Collection<ServiceRegistry> registries) {
        for (ServiceRegistry registry : registries) {
            this.bindRegistry(registry);
        }
    }

    public void PushPath(Instance instance) {
        path.add(instance);
    }

    private void bindRegistry(ServiceRegistry registry) {

    }

    private <T> Supplier<T> FindResolver(Class<T> definition) {
        return () -> null;
    }

    public static class BeanKey<T> implements BeanIdentifier {

        public BeanKey(Class<T> target) {

        }

        @Override
        public int length() {
            return 0;
        }

        @Override
        public char charAt(int i) {
            return 0;
        }

        @Override
        public CharSequence subSequence(int i, int i1) {
            return null;
        }
    }
}
