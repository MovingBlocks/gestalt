// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.gestalt.di.injection;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.terasology.gestalt.di.BeanContext;
import org.terasology.gestalt.di.DefaultBeanContext;
import org.terasology.gestalt.di.Lifetime;
import org.terasology.gestalt.di.ServiceRegistry;
import org.terasology.gestalt.di.exceptions.BeanResolutionException;
import org.terasology.gestalt.di.injection.beans.Counter1;
import org.terasology.gestalt.di.injection.beans.Counter2;
import org.terasology.gestalt.di.injection.beans.Counter3;
import org.terasology.gestalt.di.injection.beans.ICounter;
import org.terasology.gestalt.di.injection.beans.SampleQualifier;

import java.util.Optional;

public class DependencyResolutionTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void testBeanInjectionWithInterfaceFromConcreteType() {
        ServiceRegistry registry = new ServiceRegistry();
        registry.with(Counter1.class)
            .lifetime(Lifetime.Singleton);

        BeanContext beanContext = new DefaultBeanContext(registry);
        Optional<ICounter> counter = beanContext.findBean(ICounter.class);
        Assert.assertTrue(counter.isPresent());
    }

    @Test
    public void testFailedBeanResolutionWithDuplicateImplementations() {
        ServiceRegistry registry = new ServiceRegistry();
        registry.with(Counter1.class)
            .lifetime(Lifetime.Singleton);
        registry.with(Counter2.class)
            .lifetime(Lifetime.Singleton);

        BeanContext beanContext = new DefaultBeanContext(registry);
        exception.expect(BeanResolutionException.class);
        beanContext.getBean(ICounter.class);
    }

    @Test
    public void testBeanResolutionWithMultipleImplementationByConcrete() {
        ServiceRegistry registry = new ServiceRegistry();
        registry.with(Counter1.class)
            .lifetime(Lifetime.Singleton);
        registry.with(Counter2.class)
            .lifetime(Lifetime.Singleton);

        BeanContext beanContext = new DefaultBeanContext(registry);
        Assert.assertTrue(beanContext.findBean(Counter2.class).isPresent());
    }

    @Test
    public void testResolutionWithQualifier() {
        ServiceRegistry registry = new ServiceRegistry();

        registry.with(Counter3.class)
            .byQualifier(Qualifiers.byStereotype(SampleQualifier.class));
        registry.with(Counter2.class);

        BeanContext beanContext = new DefaultBeanContext(registry);
        Assert.assertTrue(beanContext.findBean(ICounter.class, Qualifiers.byStereotype(SampleQualifier.class)).isPresent());

        exception.expect(BeanResolutionException.class);
        beanContext.getBean(ICounter.class);

    }

    @Test
    public void testResolution2WithQualifier() {
        ServiceRegistry registry = new ServiceRegistry();

        registry.with(Counter3.class)
            .byQualifier(Qualifiers.byStereotype(SampleQualifier.class));
        BeanContext beanContext = new DefaultBeanContext(registry);

        Assert.assertTrue(beanContext.findBean(ICounter.class, Qualifiers.byStereotype(SampleQualifier.class)).isPresent());
        Assert.assertTrue(beanContext.findBean(Counter3.class, Qualifiers.byStereotype(SampleQualifier.class)).isPresent());
        Assert.assertTrue(beanContext.findBean(Counter3.class).isPresent());
        Assert.assertTrue(beanContext.findBean(ICounter.class).isPresent());
    }

}
