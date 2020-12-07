package org.terasology.gestalt.di;

import org.terasology.context.AnnotationMetadata;
import org.terasology.context.Argument;
import org.terasology.context.BeanDefinition;
import org.terasology.gestalt.di.qualifiers.Qualifier;
import org.terasology.gestalt.di.qualifiers.Qualifiers;

public final class BeanKeys {
    private BeanKeys() {
    }

    public static <T> BeanKey<T> resolveBeanKey(BeanDefinition<T> beanDefinition) {
        return resolveBeanKey(beanDefinition.targetClass(), beanDefinition.getAnnotationMetadata());
    }

    public static <T> BeanKey<T> resolveBeanKey(Class<T> clazz, Argument<T> argument) {
        Qualifier qualifier = Qualifiers.resolveQualifier(argument.getAnnotation());
        return new BeanKey<T>(clazz, qualifier);
    }

    public static <T> BeanKey<T> resolveBeanKey(Class<T> clazz, AnnotationMetadata metadata) {
        Qualifier qualifier = Qualifiers.resolveQualifier(metadata);
        return new BeanKey<T>(clazz, qualifier);
    }
}
