package org.terasology.gestalt.dependencyinjection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A {@link GameLogicProvider} annotation is used to mark interfaces that list types that should be generated
 * as singletons and made available to the game engine or other game components. This annotation also
 * allows the provider to be linked to any other providers that it depends on.
 *
 * A GameLogicProvider should have a list of methods with no arguments, each of which returns
 * an object. The types of these objects must have
 * <ul>
 *     <li>An empty constructor, a single constructor with arguments or a single constructor annotated with {@link javax.inject.Inject}</li>
 *     <li>The parameters for the constructor must be of types available either from the same GameLogicProvider, or one of the
 *     GameLogicProviders that it depends on.</li>
 * </ul>
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface GameLogicProvider {
    Class[] dependsOn() default {};
}
