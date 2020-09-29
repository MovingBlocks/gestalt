package org.terasology.gestalt.di;

import java.util.Objects;

public class DefaultBeanIdentifier implements BeanIdentifier {
    private final String id;

    public DefaultBeanIdentifier(String id) {
        this.id = id;
    }

    public String getName() {
        return id;
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public int length() {
        return id.length();
    }

    @Override
    public char charAt(int i) {
        return id.charAt(i);
    }

    @Override
    public CharSequence subSequence(int i, int i1) {
        return id.substring(i, i1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefaultBeanIdentifier that = (DefaultBeanIdentifier) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
