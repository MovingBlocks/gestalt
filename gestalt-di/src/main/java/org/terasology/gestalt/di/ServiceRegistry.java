package org.terasology.gestalt.di;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

public class ServiceRegistry {

    protected List<InstanceExpression<?>> instanceExpressions = new ArrayList<>();
    protected List<ScannerExpression> scanners = new ArrayList<>();
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

    public ScannerExpression registerScanner(ClassLoader loader) {
        ScannerExpression expression = new ScannerExpression().byClassloader(loader);
        scanners.add(expression);
        return expression;
    }

    public ScannerExpression registerScanner(String namespace) {
        ScannerExpression expression = new ScannerExpression()
            .byNamespace(namespace);
        scanners.add(expression);
        return expression;
    }

    public ScannerExpression registerScanner(ClassLoader loader, String namespace) {
        ScannerExpression expression = new ScannerExpression()
            .byClassloader(loader)
            .byNamespace(namespace);
        scanners.add(expression);
        return expression;
    }

    public ScannerExpression registerScanner(String namespace, Class<? extends Annotation>... qualifiers) {
        ScannerExpression expression = new ScannerExpression()
            .byNamespace(namespace)
            .byQualifier(qualifiers);
        scanners.add(expression);
        return expression;
    }

    public ScannerExpression registerScanner(ClassLoader loader, String namespace, Class<? extends Annotation>... qualifiers) {
        ScannerExpression expression = new ScannerExpression()
            .byClassloader(loader)
            .byNamespace(namespace)
            .byQualifier(qualifiers);
        scanners.add(expression);
        return expression;
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

        @CanIgnoreReturnValue
        public InstanceExpression<T> use(Supplier<T> instance) {
            this.supplier = instance;
            return this;
        }

        @CanIgnoreReturnValue
        public InstanceExpression<T> use(Class<T> target) {
            this.target = target;
            return this;

        }

        @CanIgnoreReturnValue
        public InstanceExpression<T> named(String name) {
            this.name = name;
            return this;
        }

    }

    public static class ScannerExpression {
        protected HashSet<ClassLoader> classLoaders = new HashSet<>();
        protected List<Class<? extends Annotation>> annotations = new ArrayList<>();
        protected String targetNamespace = "";

        public ScannerExpression byClassloader(ClassLoader loader) {
            classLoaders.add(loader);
            return this;
        }

        public ScannerExpression byClassloader(ClassLoader... loaders) {
            this.classLoaders.addAll(Arrays.asList(loaders));
            return this;
        }

        public ScannerExpression byQualifier(Class<? extends Annotation> annotation) {
            this.annotations.add(annotation);
            return this;
        }

        public ScannerExpression byQualifier(Class<? extends Annotation>... annotation) {
            this.annotations.addAll(Arrays.asList(annotation));
            return this;
        }

        public ScannerExpression byNamespace(String namespace) {
            this.targetNamespace = namespace;
            return this;
        }
    }
}
