package org.terasology.context;

import javax.inject.Provider;

public class ProviderArgument<T> implements Argument<Provider<T>> {


    private final Class<T> type;
    private final AnnotationMetadata annotationMetadata;

    public ProviderArgument(Class<T> type, AnnotationMetadata annotationMetadata) {
        this.type = type;
        this.annotationMetadata = annotationMetadata;
    }

    @Override
    public Class<Provider<T>> getType() {
        return (Class<Provider<T>>) type;
    }

    @Override
    public AnnotationMetadata getAnnotation() {
        return annotationMetadata;
    }

    @Override
    public String getName() {
        return type.getSimpleName();
    }
}
