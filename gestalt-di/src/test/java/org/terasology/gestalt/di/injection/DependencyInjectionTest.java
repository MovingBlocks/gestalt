// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.gestalt.di.injection;

import org.junit.Assert;
import org.junit.Test;
import org.terasology.context.injection.Qualifiers;
import org.terasology.gestalt.di.BeanContext;
import org.terasology.gestalt.di.DefaultBeanContext;
import org.terasology.context.Lifetime;
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
        ICounter c1 = beanContext.getBean(ICounter.class, Qualifiers.byName("Counter1"));
        ICounter c2 = beanContext.getBean(ICounter.class, Qualifiers.byName("Counter2"));

        Assert.assertTrue(test.isPresent());

        test.get().addToCounter1();

        Assert.assertEquals(0, c2.getCount());
        Assert.assertEquals(1, c1.getCount());
    }
}
