package org.terasology.gestalt.di.function;

import org.terasology.context.AnnotationMetadata;
import org.terasology.context.Argument;

import java.lang.reflect.Parameter;

public class ReflectionArgument<T> implements Argument<T> {
    Class<T> clazz;
    public ReflectionArgument(Parameter parameter) {
        this.clazz = (Class<T>) parameter.getClass();

    }

    @Override
    public Class<T> getType() {
        return this.clazz;
    }

    @Override
    public AnnotationMetadata getAnnotation() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }
}
