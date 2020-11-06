package org.terasology.gestalt.di.instance;

import org.terasology.gestalt.di.BeanContext;

import java.util.function.Function;
import java.util.function.Supplier;

public class LambdaInstance<T> extends Instance{

    public LambdaInstance(Class<T> serviceType, Supplier<T> service) {
        super(null,null);

    }


    @Override
    public Function<BeanContext, Object> toResolve(BeanContext from) {
        return null;
    }

    @Override
    public void close() throws Exception {

    }
}
