// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.gestalt.di;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.terasology.context.Lifetime;
import org.terasology.context.injection.Qualifier;
import org.terasology.context.injection.Qualifiers;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;

/**
 * Service registry builds a definitions used {@link BeanContext} for injection.
 */
public class ServiceRegistry {
    protected List<InstanceExpression<?>> instanceExpressions = new ArrayList<>();
    protected List<BeanScanner> scanners = new ArrayList<>();
    protected Multimap<Qualifier, BeanIntercept> intercepts = HashMultimap.create();
    protected HashSet<ClassLoader> classLoaders = new HashSet<>();

    /**
     * includes another registry and add everything into this registry
     * @param registry
     */
    public void includeRegistry(ServiceRegistry registry) {
        instanceExpressions.addAll(registry.instanceExpressions);
        scanners.addAll(registry.scanners);
        classLoaders.addAll(registry.classLoaders);
        intercepts.putAll(registry.intercepts);
    }

    /**
     * add a definition to the registry by type
     * @param type the type to resolve with
     * @param <T> type
     * @return the expression for the definition
     */
    @CanIgnoreReturnValue
    public <T> InstanceExpression<T> with(Class<T> type) {
        InstanceExpression<T> expr = new InstanceExpression<>(type);
        instanceExpressions.add(expr);
        return expr.use(type);
    }

    /**
     * add a definition to the registry by type and {@link Lifetime}
     * @param type the type
     * @param lifetime lifecycle of the bean
     * @param <T>
     * @return the expression for the definition
     */
    @CanIgnoreReturnValue
    public <T> InstanceExpression<T> with(Class<T> type, Lifetime lifetime) {
        return this.with(type).lifetime(lifetime);
    }

    /**
     * register classloader that is loaded into the {@link BeanEnvironment}
     * @param loader the loader
     */
    public void registerClassLoader(ClassLoader loader) {
        classLoaders.add(loader);
    }

    /**
     * scanner will resolve definitions for this {@link ServiceRegistry} through the {@link BeanEnvironment}.
     * @param scanner the scanner to resolve definitions by
     */
    public void registerScanner(BeanScanner scanner) {
        scanners.add(scanner);
    }

    /**
     * An intercept will intercept a request before they are handled by the {@link BeanContext}.
     * @param qualifier the qualifier to filter request by
     * @param intercept the intercept
     */
    public void registerIntercept(Qualifier qualifier, BeanIntercept intercept){
        this.intercepts.put(qualifier, intercept);
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

        /**
         * the supplier used to supply the object
         * @param instance
         * @return
         */
        @CanIgnoreReturnValue
        public InstanceExpression<T> use(Supplier<T> instance) {
            this.supplier = instance;
            return this;
        }

        /**
         * the subtype of the root expression
         * @param target
         * @return
         */
        @CanIgnoreReturnValue
        public InstanceExpression<T> use(Class<? extends T> target) {
            this.target = target;
            return this;
        }

        /**
         * the qualifier to filter the expression by
         * @param qualifier the qualifier
         * @return this
         */
        @CanIgnoreReturnValue
        public InstanceExpression<T> byQualifier(Qualifier qualifier) {
            this.qualifier = qualifier;
            return this;
        }

        /**
         * stereotype by a annotation annotated by {@link Qualifier}
         * @param qualifier the qualifier
         * @return this
         */
        @CanIgnoreReturnValue
        public InstanceExpression<T> byStereotype(Class<? extends Annotation> qualifier) {
            this.qualifier = Qualifiers.byStereotype(qualifier);
            return this;
        }

        /**
         * A stereotype that is injected by {@link javax.inject.Named}
         * @param name the name of the annotation
         * @return this
         */
        @CanIgnoreReturnValue
        public InstanceExpression<T> named(String name) {
            this.qualifier = Qualifiers.byName(name);
            return this;
        }
    }
}
