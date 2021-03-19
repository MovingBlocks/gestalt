package org.terasology.gestalt.di;

import org.junit.Test;
import org.terasology.context.annotation.Service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AutoClosableTest {

    @Service
    public static class AutoClosableBean implements AutoCloseable{
        protected boolean isClosed = false;
        public AutoClosableBean() {

        }

        @Override
        public void close() throws Exception {
            isClosed = true;
        }
    }

    @Test
    public void testAutoClose() {
        ServiceRegistry registry = new ServiceRegistry();
        registry.singleton(AutoClosableBean.class);

        AutoClosableBean bean = null;
        try(BeanContext context = new DefaultBeanContext(registry)) {
            bean = context.getBean(AutoClosableBean.class);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        assertNotNull(bean);
        assertTrue(bean.isClosed);
    }
}
