// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.gestalt.di;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.terasology.gestalt.di.beans.Dep2;
import org.terasology.gestalt.di.beans.ParentDep;
import org.terasology.gestalt.di.beans.TestRegistry;

import java.util.Optional;

public class TestRegistryTests {

    BeanContext beanContext = new DefaultBeanContext(new TestRegistry());

    @Test
    public void checkDirectResolving() {
        Optional<Dep2> dep = beanContext.findBean(Dep2.class);
        Assertions.assertTrue(dep.isPresent());
        Assertions.assertTrue(dep.get() instanceof Dep2);
    }

    @Test
    public void checkChildBeanContextResolving() {
        BeanContext childBeanContext = new DefaultBeanContext(beanContext);
        Optional<Dep2> dep = childBeanContext.findBean(Dep2.class);
        Assertions.assertTrue(dep.isPresent());
        Assertions.assertTrue(dep.get() instanceof Dep2);
    }

    @Test
    public void checkDependencyBean() {
        Optional<ParentDep> dep = beanContext.findBean(ParentDep.class);
        Assertions.assertTrue(dep.isPresent());
        Assertions.assertNotNull(dep.get().getDep());
    }
}
