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
        registry.with(AbstractBeanTest.AbstractImpl1.class).lifetime(Lifetime.Singleton);
        registry.with(AbstractBeanTest.AbstractImpl2.class).lifetime(Lifetime.Singleton);
        registry.with(ConcreteImplementationFromAbs.class).lifetime(Lifetime.Singleton);

        BeanContext cntx = new DefaultBeanContext(registry);
        ConcreteImplementationFromAbs c1 = cntx.getBean(ConcreteImplementationFromAbs.class);

        Assert.assertNotNull(c1.impl);
        Assert.assertNotNull(c1.Impl2);

    }

    @Test
    public void injectAbstractBeanGetAbstract() {
        ServiceRegistry registry = new ServiceRegistry();
        registry.with(AbstractBeanTest.AbstractImpl1.class).lifetime(Lifetime.Singleton);
        registry.with(AbstractBeanTest.AbstractImpl2.class).lifetime(Lifetime.Singleton);
        registry.with(ConcreteImplementationFromAbs.class).lifetime(Lifetime.Singleton);

        BeanContext cntx = new DefaultBeanContext(registry);
        MyAbstractImplementation c1 = cntx.getBean(MyAbstractImplementation.class);

        Assert.assertNotNull(c1.impl);
    }


    @Service
    public static class AbstractImpl1 {
        public AbstractImpl1() {

        }
    }

    @Service
    public static class AbstractImpl2 {
        public AbstractImpl2() {

        }
    }

    public static abstract class MyAbstractImplementation {
        @Inject
        AbstractImpl1 impl;

        @Inject
        public MyAbstractImplementation() {

        }
    }

    public static class ConcreteImplementationFromAbs extends MyAbstractImplementation {
        @Inject
        AbstractImpl1 Impl2;

        @Inject
        public ConcreteImplementationFromAbs() {

        }

    }
}
