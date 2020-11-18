package org.terasology.gestalt.di;

import com.sun.istack.internal.Nullable;

import java.util.Arrays;
import java.util.Objects;

public class BeanKey<T> implements BeanIdentifier {
    private final Class beanType;
    private final Class[] typeArguments;
    private final int hashCode;

    public BeanKey(Class<T> beanType, @Nullable Class... typeArguments) {
        this.beanType = beanType;
        this.typeArguments = (typeArguments == null || typeArguments.length == 0) ? null : typeArguments;
        int result = Objects.hash(beanType);
        result = 31 * result + Arrays.hashCode(this.typeArguments);
        this.hashCode = result;
    }

    public <T> Class<T> getBeanType() {
        return beanType;
    }

    @Override
    public String toString() {
        return beanType.getName();
    }

    @Override
    public int length() {
        return toString().length();
    }

    @Override
    public char charAt(int i) {
        return toString().charAt(i);
    }

    @Override
    public CharSequence subSequence(int i, int i1) {
        return toString().subSequence(i, i1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BeanKey<?> beanKey = (BeanKey<?>) o;
        return hashCode == beanKey.hashCode &&
            Objects.equals(beanType, beanKey.beanType) &&
            Arrays.equals(typeArguments, beanKey.typeArguments);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }
}
