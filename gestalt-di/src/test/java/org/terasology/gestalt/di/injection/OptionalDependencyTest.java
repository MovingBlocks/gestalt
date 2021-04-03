package org.terasology.gestalt.di.injection;

import org.junit.Test;
import org.terasology.context.Lifetime;
import org.terasology.context.annotation.Service;
import org.terasology.gestalt.di.BeanContext;
import org.terasology.gestalt.di.DefaultBeanContext;
import org.terasology.gestalt.di.ServiceRegistry;

import javax.inject.Inject;
import java.util.Optional;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class OptionalDependencyTest {
    @Service
    public static class TargetOptionalDep1Test {
    }

    @Service
    public static class TargetOptionalDep2Test {
    }

    public static class TargetOptionalDep {
        public Optional<TargetOptionalDep1Test> dep1Test;
        public Optional<TargetOptionalDep2Test> dep2Test;
        @Inject
        public TargetOptionalDep(Optional<TargetOptionalDep1Test> dep1, Optional<TargetOptionalDep2Test> dep2) {
            this.dep1Test = dep1;
            this.dep2Test = dep2;
        }
    }

    @Test
    public void testBeanInjectionWith1Optional() {
        ServiceRegistry registry = new ServiceRegistry();
        registry.with(TargetOptionalDep1Test.class, Lifetime.Singleton);
        registry.with(TargetOptionalDep.class, Lifetime.Singleton);

        BeanContext beanContext = new DefaultBeanContext(registry);
        TargetOptionalDep bean = beanContext.getBean(TargetOptionalDep.class);
        assertTrue(bean.dep1Test.isPresent());
        assertFalse(bean.dep2Test.isPresent());
    }

    @Test
    public void testBeanInjectionWithBothOptional() {
        ServiceRegistry registry = new ServiceRegistry();
        registry.with(TargetOptionalDep1Test.class, Lifetime.Singleton);
        registry.with(TargetOptionalDep2Test.class, Lifetime.Singleton);
        registry.with(TargetOptionalDep.class, Lifetime.Singleton);

        BeanContext beanContext = new DefaultBeanContext(registry);
        TargetOptionalDep bean = beanContext.getBean(TargetOptionalDep.class);
        assertTrue(bean.dep1Test.isPresent());
        assertTrue(bean.dep2Test.isPresent());
    }
}
