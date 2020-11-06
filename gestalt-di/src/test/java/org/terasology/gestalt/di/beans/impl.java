package org.terasology.gestalt.di.beans;

import org.terasology.gestalt.di.BeanContext;
import org.terasology.gestalt.di.DefaultBeanContext;

public class impl {
    void main() {
        BeanContext context = new DefaultBeanContext(new TestRegistry());
    }
}
