package org.terasology.gestalt.di.injection;

import org.junit.Assert;
import org.junit.Test;
import org.terasology.gestalt.di.BeanContext;
import org.terasology.gestalt.di.DefaultBeanContext;
import org.terasology.gestalt.di.Lifetime;
import org.terasology.gestalt.di.ServiceRegistry;
import org.terasology.gestalt.di.injection.beans.Counter1;
import org.terasology.gestalt.di.injection.beans.Counter2;
import org.terasology.gestalt.di.injection.beans.CounterTester;
import org.terasology.gestalt.di.injection.beans.ICounter;

import java.util.Optional;

public class DependencyInjectionTest {

    @Test
    public void testBeanInjection() {
        ServiceRegistry registry = new ServiceRegistry();
        registry.with(ICounter.class)
            .use(Counter1.class)
            .lifetime(Lifetime.Singleton)
            .named("Counter1");
        registry.with(ICounter.class)
            .use(Counter2.class)
            .lifetime(Lifetime.Singleton)
            .named("Counter2");

        registry.with(CounterTester.class).lifetime(Lifetime.Singleton);

        BeanContext beanContext = new DefaultBeanContext(registry);
        Optional<CounterTester> test = beanContext.findBean(CounterTester.class);
        Optional<ICounter> c1 = beanContext.findBean(ICounter.class, Qualifiers.byName("Counter1"));
        Optional<ICounter> c2 = beanContext.findBean(ICounter.class, Qualifiers.byName("Counter2"));

        Assert.assertTrue(test.isPresent());
        Assert.assertTrue(c1.isPresent());
        Assert.assertTrue(c2.isPresent());

        test.get().addToCounter1();

        Assert.assertEquals(0, c2.get().getCount());
        Assert.assertEquals(1, c1.get().getCount());
    }
}
