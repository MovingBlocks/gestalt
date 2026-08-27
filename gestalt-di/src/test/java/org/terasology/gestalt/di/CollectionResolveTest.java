// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.gestalt.di;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.terasology.context.Lifetime;
import org.terasology.context.annotation.Service;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class CollectionResolveTest {

    @Test
    public void getMultipleBeans() {
        ServiceRegistry registry = new ServiceRegistry();
        registry.with(Impl1.class).lifetime(Lifetime.Singleton);
        registry.with(Impl2.class).lifetime(Lifetime.Singleton);

        BeanContext cntx = new DefaultBeanContext(registry);

        List<SomeThing> result = cntx.getBeans(SomeThing.class);
        Assertions.assertFalse(result.isEmpty());
        Assertions.assertArrayEquals(
                new Object[]{Impl1.class, Impl2.class},
                result.stream()
                        .map(Object::getClass)
                        .sorted(Comparator.comparing(Class::getName))
                        .toArray());
    }

    @Test
    public void checkListInject() {
        ServiceRegistry registry = new ServiceRegistry();
        registry.with(Impl1.class).lifetime(Lifetime.Singleton);
        registry.with(Impl2.class).lifetime(Lifetime.Singleton);
        registry.with(ListUsageBean.class).lifetime(Lifetime.Singleton);

        BeanContext cntx = new DefaultBeanContext(registry);

        ListUsageBean result = cntx.getBean(ListUsageBean.class);
        Assertions.assertFalse(result.list.isEmpty());
        Assertions.assertArrayEquals(
                new Object[]{Impl1.class, Impl2.class},
                result.list.stream()
                        .map(Object::getClass)
                        .sorted(Comparator.comparing(Class::getName))
                        .toArray());
    }

    @Test
    public void checkSetInject() {
        ServiceRegistry registry = new ServiceRegistry();
        registry.with(Impl1.class).lifetime(Lifetime.Singleton);
        registry.with(Impl2.class).lifetime(Lifetime.Singleton);
        registry.with(SetUsageBean.class).lifetime(Lifetime.Singleton);

        BeanContext cntx = new DefaultBeanContext(registry);

        SetUsageBean result = cntx.getBean(SetUsageBean.class);
        Assertions.assertFalse(result.set.isEmpty());
        Assertions.assertArrayEquals(
                new Object[]{Impl1.class, Impl2.class},
                result.set.stream()
                        .map(Object::getClass)
                        .sorted(Comparator.comparing(Class::getName))
                        .toArray());
    }

    @Test
    public void checkCollectionInject() {
        ServiceRegistry registry = new ServiceRegistry();
        registry.with(Impl1.class).lifetime(Lifetime.Singleton);
        registry.with(Impl2.class).lifetime(Lifetime.Singleton);
        registry.with(CollectionUsageBean.class).lifetime(Lifetime.Singleton);

        BeanContext cntx = new DefaultBeanContext(registry);

        CollectionUsageBean result = cntx.getBean(CollectionUsageBean.class);
        Assertions.assertFalse(result.collection.isEmpty());
        Assertions.assertArrayEquals(
                new Object[]{Impl1.class, Impl2.class},
                result.collection.stream()
                        .map(Object::getClass)
                        .sorted(Comparator.comparing(Class::getName))
                        .toArray());
    }

    @Service
    public interface SomeThing {

    }

    @Service
    public static class ListUsageBean {
        private final List<SomeThing> list;

        @Inject
        public ListUsageBean(List<SomeThing> list) {
            this.list = list;
        }
    }

    @Service
    public static class SetUsageBean {
        private final Set<SomeThing> set;

        @Inject
        public SetUsageBean(Set<SomeThing> set) {
            this.set = set;
        }
    }

    @Service
    public static class CollectionUsageBean {
        private final Collection<SomeThing> collection;

        @Inject
        public CollectionUsageBean(Collection<SomeThing> collection) {
            this.collection = collection;
        }
    }

    @Service
    public static class Impl1 implements SomeThing {

    }

    @Service
    public static class Impl2 implements SomeThing {

    }
}
