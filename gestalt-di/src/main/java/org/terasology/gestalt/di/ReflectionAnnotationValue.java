package org.terasology.gestalt.di;

import org.terasology.context.AnnotationValue;

import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public class ReflectionAnnotationValue<S extends Annotation> implements AnnotationValue<S> {
    public ReflectionAnnotationValue(Annotation annotation){

    }

    @Override
    public String getAnnotationName() {
        return null;
    }

    @Override
    public Class<S> getAnnotationType() {
        return null;
    }

    @Override
    public boolean hasAnnotation(String annotation) {
        return false;
    }

    @Override
    public boolean hasAnnotation(Class<? extends Annotation> annotation) {
        return false;
    }

    @Override
    public <T extends Annotation> AnnotationValue<T>[] getAnnotation(Class<T> annotation) {
        return new AnnotationValue[0];
    }

    @Override
    public AnnotationValue<Annotation>[] getAnnotation(String annotation) {
        return new AnnotationValue[0];
    }

    @Override
    public boolean hasStereotype(Class<? extends Annotation> ann) {
        return false;
    }

    @Override
    public boolean hasStereotype(String ann) {
        return false;
    }

    @Override
    public List<AnnotationValue<Annotation>> getAnnotationsByStereotype(String stereotype) {
        return null;
    }

    @Override
    public List<AnnotationValue<Annotation>> getAnnotationsByStereotype(Class<? extends Annotation> stereotype) {
        return null;
    }

    @Override
    public List<AnnotationValue<Annotation>> findAnnotations(String annotation) {
        return null;
    }

    @Override
    public List<AnnotationValue<Annotation>> findAnnotations(Class<? extends Annotation> annotation) {
        return null;
    }

    @Override
    public OptionalInt intValue(String field) {
        return null;
    }

    @Override
    public Optional<String> stringValue(String field) {
        return Optional.empty();
    }

    @Override
    public boolean hasField(String field) {
        return false;
    }

    @Override
    public Object getRawValue(String field) {
        return null;
    }

    @Override
    public Iterator<AnnotationValue<Annotation>[]> iterator() {
        return null;
    }
}
