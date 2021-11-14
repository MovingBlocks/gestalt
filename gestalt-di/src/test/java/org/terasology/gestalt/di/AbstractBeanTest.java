package org.terasology.gestalt.di;

import org.junit.Assert;
import org.junit.Test;
import org.terasology.context.Lifetime;
import org.terasology.context.annotation.Service;

import javax.inject.Inject;

public class AbstractBeanTest {
    @Test
    public void injectAbstractBean() {
        ServiceRegistry registry = new ServiceRegistry();
        registry.with(AbstractBeanTest.Impl1.class).lifetime(Lifetime.Singleton);
        registry.with(AbstractBeanTest.Impl2.class).lifetime(Lifetime.Singleton);
        registry.with(ConcreteImplementationFromAbs.class).lifetime(Lifetime.Singleton);

        BeanContext cntx = new DefaultBeanContext(registry);
        ConcreteImplementationFromAbs c1 = cntx.getBean(ConcreteImplementationFromAbs.class);

        Assert.assertNotNull(c1.impl);
        Assert.assertNotNull(c1.Impl2);

    }

    @Service
    public static class Impl1 {
        public Impl1() {

        }
    }

    @Service
    public static class Impl2 {
        public Impl2() {

        }
    }

    public static abstract class MyAbstractImplementation {
        @Inject
        Impl1 impl;

        @Inject
        public MyAbstractImplementation() {

        }
    }

    public static class ConcreteImplementationFromAbs extends MyAbstractImplementation {
        @Inject
        Impl1 Impl2;

        @Inject
        public ConcreteImplementationFromAbs() {

        }

    }
}
