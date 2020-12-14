package org.terasology.gestalt.di;

import org.terasology.context.Argument;
import org.terasology.gestalt.di.qualifiers.Qualifier;
import org.terasology.gestalt.di.qualifiers.Qualifiers;

public final class BeanKeys {
    private BeanKeys() {
    }

    public static <T> BeanKey<T> resolveBeanKey(Class<T> clazz, Argument<T> argument) {
        Qualifier qualifier = Qualifiers.resolveQualifier(argument.getAnnotation());
        return new BeanKey<T>(clazz, qualifier);
    }
}
