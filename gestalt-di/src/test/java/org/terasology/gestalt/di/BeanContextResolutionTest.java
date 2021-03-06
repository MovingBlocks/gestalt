// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.gestalt.di;

import org.junit.Test;
import org.terasology.context.Lifetime;
import org.terasology.gestalt.di.beans.ContextDep;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BeanContextResolutionTest {
    @Test
    public void testContextInject1() {
        ServiceRegistry registry = new ServiceRegistry();
        registry.with(ContextDep.class).lifetime(Lifetime.Singleton);
        BeanContext beanContext = new DefaultBeanContext(registry);
        BeanContext child = beanContext.getNestedContainer();

        Optional<ContextDep> target = child.findBean(ContextDep.class);
        assertTrue(target.isPresent());
        assertNotNull(target.get().context);
        assertEquals(target.get().context, beanContext);

    }



}
