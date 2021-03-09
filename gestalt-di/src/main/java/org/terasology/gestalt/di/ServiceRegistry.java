// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.gestalt.di;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.terasology.gestalt.di.injection.Qualifier;
import org.terasology.gestalt.di.injection.Qualifiers;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;

public class ServiceRegistry {

    protected List<InstanceExpression<?>> instanceExpressions = new ArrayList<>();
    protected List<BeanScanner> scanners = new ArrayList<>();
    protected Multimap<Qualifier, BeanIntercept> intercepts = HashMultimap.create();
    protected HashSet<ClassLoader> classLoaders = new HashSet<>();

    public void includeRegistry(ServiceRegistry registry) {
        instanceExpressions.addAll(registry.instanceExpressions);
        scanners.addAll(registry.scanners);
        classLoaders.addAll(registry.classLoaders);
    }

    @CanIgnoreReturnValue
    public <T> InstanceExpression<T> with(Class<T> type) {
        InstanceExpression<T> expr = new InstanceExpression<>(type);
        instanceExpressions.add(expr);
        return expr.use(type);
    }

    public void registerClassLoader(ClassLoader loader) {
        classLoaders.add(loader);
    }

    public void registerScanner(BeanScanner scanner) {
        scanners.add(scanner);
    }

    public void registerIntercept(Qualifier qualifier, BeanIntercept intercept){
        this.intercepts.put(qualifier, intercept);
    }

    public <T> InstanceExpression<T> singleton(Class<T> type) {
        return this.with(type).lifetime(Lifetime.Singleton);
    }

    public static class InstanceExpression<T> {
        protected final Class<T> root;
        protected Class<? extends T> target;
        protected Lifetime lifetime;
        protected Supplier<? extends T> supplier;
        protected Qualifier<?> qualifier;

        public InstanceExpression<T> lifetime(Lifetime lifetime) {
            this.lifetime = lifetime;
            return this;
        }

        public InstanceExpression(Class<T> root) {
            this.root = root;
            this.target = root;
            lifetime = Lifetime.Scoped;
        }

        @CanIgnoreReturnValue
        public InstanceExpression<T> use(Supplier<T> instance) {
            this.supplier = instance;
            return this;
        }

        @CanIgnoreReturnValue
        public InstanceExpression<T> use(Class<? extends T> target) {
            this.target = target;
            return this;
        }

        @CanIgnoreReturnValue
        public InstanceExpression<T> byQualifier(Qualifier qualifier) {
            this.qualifier = qualifier;
            return this;
        }

        @CanIgnoreReturnValue
        public InstanceExpression<T> byStereotype(Class<? extends Annotation> qualifier) {
            this.qualifier = Qualifiers.byStereotype(qualifier);
            return this;
        }

        @CanIgnoreReturnValue
        public InstanceExpression<T> named(String name) {
            this.qualifier = Qualifiers.byName(name);
            return this;
        }
    }
}
