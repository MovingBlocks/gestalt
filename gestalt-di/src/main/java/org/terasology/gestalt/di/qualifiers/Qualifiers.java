package org.terasology.gestalt.di.qualifiers;

import org.terasology.context.AnnotationMetadata;
import org.terasology.context.AnnotationValue;
import org.terasology.context.Argument;

import javax.inject.Named;
import java.util.Optional;

public class Qualifiers {
    public static <T> Qualifier<T> byName(String name) {
        return new NameQualifier<T>(name);
    }

    public static <T> Qualifier<T>  resolveQualifier(Argument<T> argument) {
        return resolveQualifier(argument.getAnnotation());
    }

    public static <T> Qualifier<T>  resolveQualifier(AnnotationMetadata metadata) {
        if (metadata.hasStereotype(javax.inject.Qualifier.class)) {
            for (AnnotationValue ann : metadata.getAnnotationsByStereotype(javax.inject.Qualifier.class)) {
                return new StereotypeQualifier(ann.getAnnotationType());
            }
        }
        for (AnnotationValue target : metadata.findAnnotations(Named.class)) {
            Optional<String> value = target.stringValue("value");
            if (value.isPresent()) {
                return new NameQualifier<>(value.get());
            }
        }
        return null;
    }}
