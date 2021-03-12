package org.terasology.gestalt.di.index;

import java.util.Set;

/**
 * Index for classes, which provide `gestalt-di`.
 * You should gather index via `gestalt-inject-java` annotation processing before use this class.
 */
public interface ClassIndex {
    Set<String> getSubtypesOf(String clazzName);

    Set<String> getTypesAnnotatedWith(String annotation);
}
