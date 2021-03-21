package org.terasology.gestalt.di;

import org.terasology.context.AnnotationMetadata;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * intercepts a bean as its request from the context
 * @param <R>
 */
public interface BeanIntercept<R> {
    /**
     * A single instance request single instance for {@link BeanContext#getBean}. An empty optional is ignored and is request normally from the context.
     *
     * @param key        the bean key associated with the request
     * @param annotation The annotation data
     * @return the intercepted result
     */
    Optional<R> single(BeanKey key, AnnotationMetadata annotation);

    /**
     * Multiples instance are request collection for {@link BeanContext#getBeans}. An empty optional is ignored and the request form beankey is processed normally unless handled by another intercept.
     *
     * @param key        the bean key associated with the request
     * @param annotation the annotation data
     * @return the intercepted result
     */
    Optional<Stream<R>> collection(BeanKey key, AnnotationMetadata annotation);
}
