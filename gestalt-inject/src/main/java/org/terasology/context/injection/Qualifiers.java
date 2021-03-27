// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.context.injection;

import org.terasology.context.AnnotationMetadata;
import org.terasology.context.AnnotationValue;
import org.terasology.context.Argument;

import javax.inject.Named;
import java.lang.annotation.Annotation;
import java.util.Optional;

public final class Qualifiers {
    private Qualifiers() {

    }

    public static <T> Qualifier<T> byName(String name) {
        return new NameQualifier(name);
    }

    public static <T> Qualifier<T> byStereotype(Class<? extends Annotation> ann) {
        return new StereotypeQualifier(ann);
    }

    public static <T> Qualifier<T> resolveQualifier(Argument<T> argument) {
        return resolveQualifier(argument.getAnnotation());
    }

    public static <T> Qualifier<T> resolveQualifier(AnnotationMetadata metadata) {
        for (AnnotationValue<Named> target : metadata.findAnnotations(Named.class)) {
            Optional<String> value = target.stringValue("value");
            if (value.isPresent()) {
                return new NameQualifier(value.get());
            }
        }
        if (metadata.hasStereotype(javax.inject.Qualifier.class)) {
            AnnotationValue<Annotation> ann = metadata.getAnnotationsByStereotype(javax.inject.Qualifier.class).stream().findFirst().get();
            return new StereotypeQualifier(ann.getAnnotationType());
        }
        return null;
    }
}
