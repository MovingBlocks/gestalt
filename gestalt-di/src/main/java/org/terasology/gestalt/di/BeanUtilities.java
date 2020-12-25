package org.terasology.gestalt.di;

import org.terasology.context.AnnotationMetadata;
import org.terasology.context.AnnotationValue;
import org.terasology.context.Argument;
import org.terasology.context.annotation.Scoped;
import org.terasology.context.annotation.Transient;
import org.terasology.gestalt.di.qualifiers.Qualifier;
import org.terasology.gestalt.di.qualifiers.Qualifiers;

import javax.inject.Singleton;
import java.lang.annotation.Annotation;

public final class BeanUtilities {
    private BeanUtilities() {

    }

    public static <T> BeanKey<?> resolveBeanKey(Class<?> clazz, Argument<?> argument) {
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
