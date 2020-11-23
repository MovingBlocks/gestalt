package org.terasology.gestalt.di.instance;

import org.terasology.context.AnnotationMetadata;
import org.terasology.context.Argument;
import org.terasology.gestalt.di.BeanKey;

import javax.inject.Qualifier;

public final class InjectionUtility {
    private InjectionUtility() {
    }

    public static <T> BeanKey<T> resolveBeanKey(Argument<T> argument){

        AnnotationMetadata metadata = argument.getAnnotation();
        if(metadata.hasStereotype(Qualifier.class)) {
//            metadata.getAnnotationsByStereotype(Qualifier.class)
        }


        return new BeanKey<T>(argument.getType(), null);
    }
}
