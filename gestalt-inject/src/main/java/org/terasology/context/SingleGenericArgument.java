package org.terasology.context;

public class SingleGenericArgument<T> implements Argument<T> {

    private final Class<T> type;
    private final AnnotationMetadata annotationMetadata;

    public SingleGenericArgument(Class<T> type, AnnotationMetadata annotationMetadata) {
        this.type = type;
        this.annotationMetadata = annotationMetadata;
    }

    @Override
    public Class<T> getType() {
        return type;
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
