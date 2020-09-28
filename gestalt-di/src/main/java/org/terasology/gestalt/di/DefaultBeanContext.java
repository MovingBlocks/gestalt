package org.terasology.gestalt.di;

import org.terasology.context.BeanContext;
import org.terasology.context.BeanDefinition;
import org.terasology.context.SoftServiceLoader;

import java.util.HashMap;
import java.util.Map;

public class DefaultBeanContext extends BeanContext implements AutoCloseable {
    private final Map<Class, BeanDefinition> definitions = new HashMap<>();

    public BeanContext getParent(){
        return null;
    }

    public<T> T inject(T instance){
        BeanDefinition definition = definitions.get(instance.getClass());
        //TODO: setup inject
        return null;
    }

    public DefaultBeanContext() {
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
