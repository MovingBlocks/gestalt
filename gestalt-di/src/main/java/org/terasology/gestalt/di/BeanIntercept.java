package org.terasology.gestalt.di;

import org.terasology.context.AnnotationMetadata;

import java.util.Optional;
import java.util.stream.Stream;

public interface BeanIntercept<R> {
    Optional<R> single(BeanKey key, AnnotationMetadata annotation);
    Optional<Stream<R>> collection(BeanKey key, AnnotationMetadata annotation);
}
