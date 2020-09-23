package org.terasology.gestalt.di.annotation;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
public @interface AnnotationValue {
    String one() default "one";
    String two() default "";
    int three() default 3;
}
