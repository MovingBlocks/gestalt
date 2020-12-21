package org.terasology.gestalt.di;

import org.junit.Assert;
import org.junit.Test;
import org.terasology.gestalt.di.beans.Dep2;
import org.terasology.gestalt.di.beans.ParentDep;
import org.terasology.gestalt.di.beans.TestRegistry;

import java.util.Optional;

public class TestRegistryTests {

    BeanContext beanContext = new DefaultBeanContext(new TestRegistry());

    @Test
    public void checkDirectResolving() {
        Optional<Dep2> dep = beanContext.getBean(Dep2.class);
        Assert.assertTrue(dep.isPresent());
        Assert.assertTrue(dep.get() instanceof Dep2);

    }

    @Test
    public void checkChildBeanContextResolving() {
        BeanContext childBeanContext = new DefaultBeanContext(beanContext);
        Optional<Dep2> dep = childBeanContext.getBean(Dep2.class);
        Assert.assertTrue(dep.isPresent());
        Assert.assertTrue(dep.get() instanceof Dep2);
    }

    @Test
    public void checkDependencyBean() {
        Optional<ParentDep> dep = beanContext.getBean(ParentDep.class);
        Assert.assertTrue(dep.isPresent());
        Assert.assertNotNull(dep.get().getDep());
    }
}
