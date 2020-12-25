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
    public class ScannerRegistry extends ServiceRegistry {
        public ScannerRegistry() {
            this.registerScanner(new StandardScanner("org.terasology.gestalt.di.scanner.standard"));
        }
    }
    @Test
    public void checkBeanRegistry() {
        BeanContext beanContext = new DefaultBeanContext(new ScannerRegistry());

        Optional<SingletonBean> bean = beanContext.getBean(SingletonBean.class);
        Optional<SingletonBean> bean2 = beanContext.getBean(SingletonBean.class);
        Assert.assertTrue(bean.isPresent());
        Assert.assertTrue(bean2.isPresent());
        Assert.assertTrue(bean.get() == bean2.get());
    }

}
