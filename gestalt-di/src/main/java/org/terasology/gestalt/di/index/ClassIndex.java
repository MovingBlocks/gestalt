package org.terasology.gestalt.di.index;

import java.util.Set;

public interface ClassIndex {
    Set<String> getSubtypesOf(String clazzName);

    Set<String> getTypesAnnotatedWith(String annotation);
}
