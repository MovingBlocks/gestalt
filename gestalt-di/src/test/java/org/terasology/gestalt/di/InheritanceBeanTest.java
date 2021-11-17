package org.terasology.gestalt.di;

import org.junit.Assert;
import org.junit.Test;
import org.terasology.context.Lifetime;
import org.terasology.context.annotation.Service;

import javax.inject.Inject;

public class InheritanceBeanTest {

    @Test
    public void TestInjectWithInheritance() {
        ServiceRegistry registry = new ServiceRegistry();
        registry.with(InheritanceBeanA1.class).lifetime(Lifetime.Singleton);
        registry.with(InheritanceBeanA2.class).lifetime(Lifetime.Singleton);
        registry.with(InheritanceBeanB2.class).lifetime(Lifetime.Singleton);

        BeanContext cntx = new DefaultBeanContext(registry);
        InheritanceBeanB2 b2 = cntx.getBean(InheritanceBeanB2.class);

        Assert.assertNotNull(b2.a1);
        Assert.assertNotNull(b2.a2);
    }


    @Service
    public static class InheritanceBeanA1 {}
    @Service
    public static class InheritanceBeanA2 {}
    @Service
    public static class InheritanceBeanB1 {
        @Inject
        InheritanceBeanA1 a1;
    }
    @Service
    public static class InheritanceBeanB2 extends InheritanceBeanB1 {
        @Inject
        InheritanceBeanA2 a2;
    }

}
