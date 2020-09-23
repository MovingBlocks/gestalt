package org.terasology.gestalt.di.annotation;

import javax.inject.Singleton;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@SubAnnotation
@AnnotationValue(one = "five")
@Retention(RUNTIME)
public @interface TestAnnotation {
}
