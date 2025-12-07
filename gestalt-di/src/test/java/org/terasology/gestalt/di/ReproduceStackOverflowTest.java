package org.terasology.gestalt.di;

import org.junit.Test;
import org.terasology.context.Lifetime;

public class ReproduceStackOverflowTest {
    @Test
    public void testRecursiveClose() {
        ServiceRegistry registry = new ServiceRegistry();
        registry.with(RecursiveClosable.class).lifetime(Lifetime.Singleton).use(() -> new RecursiveClosable());

        DefaultBeanContext context = new DefaultBeanContext(registry);
        // Ensure the bean is initialized (and thus strictly bound for closing)
        RecursiveClosable bean = context.getBean(RecursiveClosable.class);
        bean.setContext(context);

        try {
            context.close();
        } catch (StackOverflowError e) {
            throw new RuntimeException("StackOverflow caught", e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class RecursiveClosable implements AutoCloseable {
        private DefaultBeanContext context;

        public void setContext(DefaultBeanContext context) {
            this.context = context;
        }

        @Override
        public void close() throws Exception {
            if (context != null) {
                // This call triggers the recursion
                context.close();
            }
        }
    }
}
