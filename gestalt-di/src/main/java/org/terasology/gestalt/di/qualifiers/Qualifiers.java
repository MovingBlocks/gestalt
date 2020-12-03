package org.terasology.gestalt.di.qualifiers;

import org.terasology.context.AnnotationMetadata;
import org.terasology.context.AnnotationValue;
import org.terasology.context.Argument;
import org.terasology.context.BeanDefinition;

import javax.inject.Named;

public class Qualifiers {
    public static <T> Qualifier<T> byName(String name) {
        return null;
    }

    public static <T> Qualifier<T>  resolveQualifier(Argument<T> argument) {
        return null;
    }

    public static <T> Qualifier<T>  resolveQualifier(BeanDefinition<T> argument) {
        argument.getAnnotationMetadata();
        return null;
    }


    public static <T> Qualifier<T>  resolveQualifier(AnnotationMetadata metadata) throws ClassNotFoundException {
        if(metadata.hasAnnotation(Named.class)) {

        } else if(metadata.hasStereotype(javax.inject.Qualifier.class)) {
            for(AnnotationValue ann: metadata.getAnnotationsByStereotype(javax.inject.Qualifier.class)) {
                if(ann.hasAnnotation(javax.inject.Qualifier.class)){
                    new StereotypeQualifier(Class.forName(ann.getAnnotationName()));

                }

            }

        }

        return null;
    }
}
