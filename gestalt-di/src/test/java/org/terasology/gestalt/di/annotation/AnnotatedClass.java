package org.terasology.gestalt.di.annotation;

import javax.inject.Singleton;

@Singleton
@TestAnnotation
@AnnotationValue(one = "hello world")
public class AnnotatedClass {
}
