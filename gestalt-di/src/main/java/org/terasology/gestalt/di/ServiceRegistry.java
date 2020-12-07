package org.terasology.gestalt.di;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

public class ServiceRegistry {

    Callable<ScannerExpression> expression;
    protected List<InstanceExpression> instanceExpressions = new ArrayList<>();

    public static class ScanExpression {

    }
    public void scanner(Callable<ScannerExpression> expression) {
        this.expression = expression;
    }

    public void includeRegistry(ServiceRegistry registry) {
        for (InstanceExpression expression : registry.instanceExpressions) {
            instanceExpressions.add(expression);
        }
    }

    public <T> InstanceExpression<T> with(Class<T> type) {
        InstanceExpression expr = new InstanceExpression<>(type);
        instanceExpressions.add(expr);
        return expr;
    }

    public <T> InstanceExpression<T> singleton(Class<T> type) {
        return this.with(type).lifetime(Lifetime.Singleton);
    }

    public static class InstanceExpression<T> {
        protected final Class<T> root;
        protected Lifetime lifetime;
        protected String name;
        protected Supplier<? extends T> supplier;
        protected Class<? extends T> target;


        public InstanceExpression<T> lifetime(Lifetime lifetime) {
            this.lifetime = lifetime;
            return this;
        }

        public InstanceExpression(Class<T> root) {
            this.root = root;
            lifetime = Lifetime.Scoped;
        }

        public InstanceExpression<T> use(Supplier<T> instance) {
            this.supplier = instance;
            this.target = null;
            return this;
        }

        public InstanceExpression<T> use(Class<T> target) {
            this.target = target;
            this.supplier = null;
            return this;

        }

        public InstanceExpression<T> named(String name) {
            this.name = name;
            return this;
        }

    }

    public static class ScannerExpression {
        ClassLoader[] classLoaders = {};
        Class<? extends Annotation> annotation;

        public void currentAssembly() {

        }

        public void withClassLoader(ClassLoader loader){
            classLoaders = new ClassLoader[]{loader};
        }

        public void withClassLoader(ClassLoader ... loaders) {
            this.classLoaders = loaders;
        }

        public void byQualifier(Class<? extends Annotation> annotation) {
            this.annotation = annotation;
        }
    }

}
