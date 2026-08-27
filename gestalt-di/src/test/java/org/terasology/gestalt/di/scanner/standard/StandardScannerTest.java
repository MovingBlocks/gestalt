// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.gestalt.di.scanner.standard;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.terasology.gestalt.di.BeanContext;
import org.terasology.gestalt.di.DefaultBeanContext;
import org.terasology.gestalt.di.ServiceRegistry;
import org.terasology.gestalt.di.scanner.standard.beans.SingletonBean;
import org.terasology.gestalt.di.scanners.StandardScanner;

import java.util.Optional;

public class StandardScannerTest {
    @Test
    public void testSingletonBeanWithScanner() {
        ServiceRegistry serviceRegistry = new ServiceRegistry();
        serviceRegistry.registerScanner(new StandardScanner("org.terasology.gestalt.di.scanner.standard"));

        BeanContext beanContext = new DefaultBeanContext(serviceRegistry);

        Optional<SingletonBean> bean = beanContext.findBean(SingletonBean.class);
        Optional<SingletonBean> bean2 = beanContext.findBean(SingletonBean.class);
        Assertions.assertTrue(bean.isPresent());
        Assertions.assertTrue(bean2.isPresent());
        Assertions.assertSame(bean.get(), bean2.get());
    }

}
