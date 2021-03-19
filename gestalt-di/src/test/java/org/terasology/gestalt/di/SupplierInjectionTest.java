// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.gestalt.di;

import org.junit.Test;
import org.terasology.context.annotation.Service;
import org.terasology.gestalt.di.beans.Dep1;

import javax.inject.Inject;
import java.util.Optional;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SupplierInjectionTest {

    @Service
    public static class SupplierInjection1 {
        @Inject
        Dep1 dep1;
    }

    @Test
    public void supplierInject() {
        ServiceRegistry registry = new ServiceRegistry();
        registry.with(Dep1.class).lifetime(Lifetime.Singleton);
        registry.with(SupplierInjection1.class).use(SupplierInjection1::new).lifetime(Lifetime.Singleton);

        BeanContext cntx = new DefaultBeanContext(registry);

        Optional<SupplierInjection1> result =  cntx.findBean(SupplierInjection1.class);
        assertTrue(result.isPresent());
        assertNotNull(result.get().dep1);
    }
}
