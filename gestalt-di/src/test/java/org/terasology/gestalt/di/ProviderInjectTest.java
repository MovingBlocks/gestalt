// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.gestalt.di;

import org.junit.Test;
import org.terasology.context.Lifetime;
import org.terasology.context.annotation.Service;
import org.terasology.gestalt.di.beans.Dep1;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Optional;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ProviderInjectTest {

    @Service
    public static class ProviderInjectBean {
        @Inject
        Provider<Dep1> dep1;
    }

    @Test
    public void providerInject() {
        ServiceRegistry registry = new ServiceRegistry();
        registry.with(ProviderInjectBean.class).lifetime(Lifetime.Singleton);
        // Dep1 should be after  dependent bean. it some kind Lazy resolution.
        registry.with(Dep1.class).lifetime(Lifetime.Singleton);

        BeanContext cntx = new DefaultBeanContext(registry);

        Optional<ProviderInjectBean> result =  cntx.findBean(ProviderInjectBean.class);
        assertTrue(result.isPresent());
        assertNotNull(result.get().dep1.get());
    }
}
