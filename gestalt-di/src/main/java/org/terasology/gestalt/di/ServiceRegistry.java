// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.gestalt.di;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.terasology.gestalt.di.function.BeanFunction;
import org.terasology.gestalt.di.function.Function0;
import org.terasology.gestalt.di.function.Function1;
import org.terasology.gestalt.di.function.Function2;
import org.terasology.gestalt.di.function.Function3;
import org.terasology.gestalt.di.function.Function4;
import org.terasology.gestalt.di.function.Function5;
import org.terasology.gestalt.di.function.Function6;
import org.terasology.gestalt.di.function.Function7;
import org.terasology.gestalt.di.function.Function8;
import org.terasology.gestalt.di.injection.Qualifier;
import org.terasology.gestalt.di.injection.Qualifiers;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class ServiceRegistry {

    protected List<InstanceExpression<?>> instanceExpressions = new ArrayList<>();
    protected List<BeanScanner> scanners = new ArrayList<>();
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

    public <T> InstanceExpression<T> singleton(Class<T> type) {
        return this.with(type).lifetime(Lifetime.Singleton);
    }

    public static class InstanceExpression<T> {


        protected final Class<T> root;
        protected Class<? extends T> target;
        protected Lifetime lifetime;
        protected Qualifier<?> qualifier;

        protected BeanFunction function;

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
        public InstanceExpression<T> use(Function0<T> instance) {
            this.function = instance;
            return this;
        }
        @CanIgnoreReturnValue
        public <T1> InstanceExpression<T> use(Function1<T1,T> instance) {
            this.function = instance;
            return this;
        }
        @CanIgnoreReturnValue
        public <T1,T2> InstanceExpression<T> use(Function2<T1, T2, T> instance) {
            this.function = instance;
            return this;
        }
        @CanIgnoreReturnValue
        public <T1,T2,T3> InstanceExpression<T> use(Function3<T1,T2,T3,T> instance) {
            this.function = instance;
            return this;
        }
        @CanIgnoreReturnValue
        public <T1,T2,T3,T4> InstanceExpression<T> use(Function4<T1,T2,T3,T4,T> instance) {
            this.function = instance;
            return this;
        }
        @CanIgnoreReturnValue
        public <T1,T2,T3,T4,T5> InstanceExpression<T> use(Function5<T1,T2,T3,T4,T5,T> instance) {
            this.function = instance;
            return this;
        }
        @CanIgnoreReturnValue
        public <T1,T2,T3,T4,T5,T6> InstanceExpression<T> use(Function6<T1,T2,T3,T4,T5,T6,T> instance) {
            this.function = instance;
            return this;
        }
        @CanIgnoreReturnValue
        public <T1,T2,T3,T4,T5,T6,T7> InstanceExpression<T> use(Function7<T1,T2,T3,T4,T5,T6,T7,T> instance) {
            this.function = instance;
            return this;
        }
        @CanIgnoreReturnValue
        public <T1,T2,T3,T4,T5,T6,T7,T8> InstanceExpression<T> use(Function8<T1,T2,T3,T4,T5,T6,T7,T8,T> instance) {
            this.function = instance;
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
