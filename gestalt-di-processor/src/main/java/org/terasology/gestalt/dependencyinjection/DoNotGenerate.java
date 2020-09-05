package org.terasology.gestalt.dependencyinjection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to indicate when a GameLogicProvider annotated interface should not have an implementation
 * generated, as an implementation has been manually defined
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface DoNotGenerate {
}
