package org.terasology.gestalt.di.annotation;

import javax.inject.Qualifier;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.PACKAGE})
@Documented
@Qualifier
@WithProperties(p1 = "Hello", p2 = 5, p3 = 10, p4 = 5.0)
public @interface TestQualifier2 {
}
