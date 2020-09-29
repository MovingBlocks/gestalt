package org.terasology.gestalt.di;

import java.lang.annotation.Annotation;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

public class ServiceRegistry {

    Callable<ScannerExpression> expression;

    public static class ScanExpression {

    }

    public void scanner(Callable<ScannerExpression> expression) {
        this.expression = expression;
    }

    public void includeRegistry(ServiceRegistry registry ){

    }

    public <T> InstanceExpression<T> with(Class<T> type) {
        return new InstanceExpression<>(type);
    }

    public static class InstanceExpression<T>  {
        Class<T> root;
        Lifetime lifetime;
        public enum Lifetime {
            Scoped,
            Singleton,
            Transient
        }

        public InstanceExpression<T> lifetime(Lifetime lifetime){
            this.lifetime = lifetime;
            return this;
        }

        public InstanceExpression(Class<T> root){

        }

        public InstanceExpression<T> Scope() {

            return this;
        }

        public InstanceExpression<T> Use(T instance) {

            return this;
        }

        public InstanceExpression<T> Use(Supplier<T> instance){
            return this;

        }

        public InstanceExpression<T> Named(String name) {
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
