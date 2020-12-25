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
import org.terasology.gestalt.di.injection.beans.ICounter2;

import java.util.Optional;

public class DependencyInjectionTest {

    public static class InjectionRegistry extends ServiceRegistry {
        public InjectionRegistry() {
            this.with(ICounter.class)
                .use(Counter1.class)
                .lifetime(Lifetime.Singleton)
                .named("Counter1");
            this.with(ICounter.class)
                .use(Counter2.class)
                .lifetime(Lifetime.Singleton)
                .named("Counter2");

            this.with(CounterTester.class).lifetime(Lifetime.Singleton);
        }
    }
    @Test
    public void testBeanInjection() {
        BeanContext beanContext = new DefaultBeanContext(new InjectionRegistry());
        Optional<CounterTester> test = beanContext.getBean(CounterTester.class);
        Optional<ICounter> c1 = beanContext.getBean(ICounter.class, Qualifiers.byName("Counter1"));
        Optional<ICounter> c2 = beanContext.getBean(ICounter.class, Qualifiers.byName("Counter2"));

        Assert.assertTrue(test.isPresent());
        Assert.assertTrue(c1.isPresent());
        Assert.assertTrue(c2.isPresent());

        test.get().addToCounter1();

        Assert.assertEquals(0,c2.get().getCount());
        Assert.assertEquals(1,c1.get().getCount());
    }
}
