package org.terasology.gestalt.di;

import org.junit.Assert;
import org.junit.Test;
import org.terasology.context.annotation.Introspected;
import org.terasology.gestalt.di.beans.Dep1;

import java.util.List;
import java.util.Optional;

public class CollectionResolveTest {

    @Test
    public void getMultipleBeans() {
        ServiceRegistry registry = new ServiceRegistry();
        registry.with(Impl1.class).lifetime(Lifetime.Singleton);
        registry.with(Impl2.class).lifetime(Lifetime.Singleton);

        BeanContext cntx = new DefaultBeanContext(registry);

        List<SomeThing> result = cntx.getBeans(SomeThing.class);
        Assert.assertFalse(result.isEmpty());
        Assert.assertArrayEquals(new Object[]{Impl1.class,Impl2.class}, result.stream().map(Object::getClass).toArray());
    }

    @Introspected
    public interface SomeThing {

    }
    @Introspected
    public static class Impl1 implements SomeThing {

    }
    @Introspected
    public static class Impl2 implements SomeThing {

    }
}
