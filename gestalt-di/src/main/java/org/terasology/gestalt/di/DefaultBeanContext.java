package org.terasology.gestalt.di;

import org.terasology.context.BeanContext;
import org.terasology.context.BeanDefinition;
import org.terasology.context.SoftServiceLoader;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class DefaultBeanContext extends BeanContext implements AutoCloseable {

    private final Stack<Lifecycle> lifecycles = new Stack<>();
    private final Map<Class, BeanDefinition> definitions = new HashMap<>();

    public void startLifecycle(Lifecycle lifecycle){
        this.lifecycles.add(lifecycle);
    }

    public void freeLifecycle(Lifecycle lifecycle) {
        do {
            Lifecycle temp = lifecycles.pop();
            temp.stop();
        }
        while (lifecycles.peek() != lifecycle);
    }

    public<T> T inject(T instance){
        BeanDefinition definition = definitions.get(instance.getClass());
        //TODO: setup inject
        return null;
    }

    public DefaultBeanContext(Environment environment) {
//        lifecycles.add(SINGLETON_LIFECYCLE);
    }

    public void loadDefinitions(ClassLoader loader) {
        SoftServiceLoader<BeanDefinition> definitions = new SoftServiceLoader<BeanDefinition>(BeanDefinition.class, loader);
        for (BeanDefinition definition : definitions) {
            this.definitions.put(definition.targetClass(), definition);
        }
    }


    private void readBeanDefinitions(){

    }

    @Override
    public void close() throws Exception {

    }
}
