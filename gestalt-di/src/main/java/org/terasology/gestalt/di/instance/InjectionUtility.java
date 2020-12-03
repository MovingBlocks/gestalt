package org.terasology.gestalt.di.instance;

import org.terasology.context.AnnotationMetadata;
import org.terasology.context.AnnotationValue;
import org.terasology.context.Argument;
import org.terasology.context.BeanDefinition;
import org.terasology.gestalt.di.BeanKey;

import javax.inject.Qualifier;

public final class InjectionUtility {
    private InjectionUtility() {
    }

    public static <T> BeanKey<T> resolveBeanKey(BeanDefinition<T> beanDefinition) {
        return null;
    }


    public static <T> BeanKey<T> resolveBeanKey(AnnotationMetadata annotationMetadata) {
        return null;
    }

    public static <T> BeanKey<T> resolveBeanKey(Argument<T> argument){

        AnnotationMetadata metadata = argument.getAnnotation();
        if(metadata.hasStereotype(Qualifier.class)) {
            for(AnnotationValue ann: metadata.getAnnotationsByStereotype(Qualifier.class)) {
                if(ann.hasAnnotation(Qualifier.class)) {

                }
            }

//            metadata.getAnnotationsByStereotype(Qualifier.class)
        }


        return new BeanKey<T>(argument.getType(), null);
    }
}
