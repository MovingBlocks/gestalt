package org.terasology.gestalt.di;

import org.junit.Assert;
import org.junit.Test;
import org.terasology.gestalt.di.beans.Dep1;
import org.terasology.gestalt.di.beans.Dep2;
import org.terasology.gestalt.di.beans.TestRegistry;

import java.util.Optional;

public class BeanContextTest {

    @Test
    public void itWorks() throws Exception {
        BeanContext beanContext=new DefaultBeanContext(new TestRegistry());
        Optional<Dep2> dep= beanContext.getBean(Dep2.class);
        Assert.assertNotNull(dep);
    }
}
