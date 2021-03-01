package org.terasology.gestalt.di.scanner.standard;

import org.junit.Assert;
import org.junit.Test;
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
        Assert.assertTrue(bean.isPresent());
        Assert.assertTrue(bean2.isPresent());
        Assert.assertSame(bean.get(), bean2.get());
    }

}
