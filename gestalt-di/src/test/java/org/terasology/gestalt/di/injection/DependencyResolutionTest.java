// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.gestalt.di.injection;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.terasology.context.injection.Qualifiers;
import org.terasology.gestalt.di.BeanContext;
import org.terasology.gestalt.di.DefaultBeanContext;
import org.terasology.context.Lifetime;
import org.terasology.gestalt.di.ServiceRegistry;
import org.terasology.gestalt.di.exceptions.BeanResolutionException;
import org.terasology.gestalt.di.injection.beans.Counter1;
import org.terasology.gestalt.di.injection.beans.Counter2;
import org.terasology.gestalt.di.injection.beans.Counter3;
import org.terasology.gestalt.di.injection.beans.ICounter;
import org.terasology.gestalt.di.injection.beans.SampleQualifier;

import java.util.Optional;

public class DependencyResolutionTest {

    @Test
    public void testBeanInjectionWithInterfaceFromConcreteType() {
        ServiceRegistry registry = new ServiceRegistry();
        registry.with(Counter1.class)
            .lifetime(Lifetime.Singleton);

        BeanContext beanContext = new DefaultBeanContext(registry);
        Optional<ICounter> counter = beanContext.findBean(ICounter.class);
        Assertions.assertTrue(counter.isPresent());
    }

    @Test
    public void testFailedBeanResolutionWithDuplicateImplementations() {
        ServiceRegistry registry = new ServiceRegistry();
        registry.with(Counter1.class)
            .lifetime(Lifetime.Singleton);
        registry.with(Counter2.class)
            .lifetime(Lifetime.Singleton);

        BeanContext beanContext = new DefaultBeanContext(registry);
        Assertions.assertThrows(BeanResolutionException.class, () -> beanContext.getBean(ICounter.class));
    }

    @Test
    public void testBeanResolutionWithMultipleImplementationByConcrete() {
        ServiceRegistry registry = new ServiceRegistry();
        registry.with(Counter1.class)
            .lifetime(Lifetime.Singleton);
        registry.with(Counter2.class)
            .lifetime(Lifetime.Singleton);

        BeanContext beanContext = new DefaultBeanContext(registry);
        Assertions.assertTrue(beanContext.findBean(Counter2.class).isPresent());
    }

    @Test
    public void testResolutionWithQualifier() {
        ServiceRegistry registry = new ServiceRegistry();

        registry.with(Counter3.class)
            .byQualifier(Qualifiers.byStereotype(SampleQualifier.class));
        registry.with(Counter2.class);

        BeanContext beanContext = new DefaultBeanContext(registry);
        Assertions.assertTrue(beanContext.findBean(ICounter.class, Qualifiers.byStereotype(SampleQualifier.class)).isPresent());

        Assertions.assertThrows(BeanResolutionException.class, () -> beanContext.getBean(ICounter.class));
    }

    @Test
    public void testResolution2WithQualifier() {
        ServiceRegistry registry = new ServiceRegistry();

        registry.with(Counter3.class)
            .byQualifier(Qualifiers.byStereotype(SampleQualifier.class));
        BeanContext beanContext = new DefaultBeanContext(registry);

        Assertions.assertTrue(beanContext.findBean(ICounter.class, Qualifiers.byStereotype(SampleQualifier.class)).isPresent());
        Assertions.assertTrue(beanContext.findBean(Counter3.class, Qualifiers.byStereotype(SampleQualifier.class)).isPresent());
        Assertions.assertTrue(beanContext.findBean(Counter3.class).isPresent());
        Assertions.assertTrue(beanContext.findBean(ICounter.class).isPresent());
    }

}
