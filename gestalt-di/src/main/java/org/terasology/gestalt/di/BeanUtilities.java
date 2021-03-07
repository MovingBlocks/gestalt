// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.gestalt.di;

import org.terasology.context.AnnotationMetadata;
import org.terasology.context.AnnotationValue;
import org.terasology.context.Argument;
import org.terasology.context.annotation.Scoped;
import org.terasology.context.annotation.Transient;
import org.terasology.gestalt.di.injection.Qualifier;
import org.terasology.gestalt.di.injection.Qualifiers;

import javax.inject.Singleton;
import java.lang.annotation.Annotation;

/**
 * A set of utilities that helps with examining {@link AnnotationMetadata}
 */
public final class BeanUtilities {
    private BeanUtilities() {

    }

    public static <T> BeanKey<? extends T> resolveBeanKey(Class<T> implClass, Class<? extends T> clazz, Argument<?> argument) {
        Qualifier<T> qualifier = Qualifiers.resolveQualifier(argument.getAnnotation());
        return new BeanKey(implClass, clazz, qualifier);
    }

    public static <T> BeanKey<T> resolveBeanKey(Class<T> clazz, Argument<?> argument) {
        Qualifier<T> qualifier = Qualifiers.resolveQualifier(argument.getAnnotation());
        return new BeanKey(clazz, qualifier);
    }

    public static Lifetime resolveLifetime(AnnotationMetadata metadata) {
        if (metadata.hasAnnotation(Singleton.class)) {
            return Lifetime.Singleton;
        } else if (metadata.hasAnnotation(Scoped.class)) {
            return Lifetime.Scoped;
        } else if (metadata.hasAnnotation(Transient.class)) {
            return Lifetime.Transient;
        }

        if (metadata.hasStereotype(javax.inject.Qualifier.class)) {
            AnnotationValue<Annotation> ann = metadata.getAnnotationsByStereotype(javax.inject.Qualifier.class).stream().findFirst().get();
            if (ann.hasAnnotation(Singleton.class)) {
                return Lifetime.Singleton;
            } else if (ann.hasAnnotation(Scoped.class)) {
                return Lifetime.Scoped;
            } else if (ann.hasAnnotation(Transient.class)) {
                return Lifetime.Transient;
            }
        }
        return Lifetime.Transient;
    }
}
