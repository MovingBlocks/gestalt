package org.terasology.gestalt.di.qualifiers;

import java.util.Objects;

public class NameQualifier<T> implements Qualifier<T> {
    private final String name;

    public NameQualifier(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NameQualifier<?> that = (NameQualifier<?>) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
