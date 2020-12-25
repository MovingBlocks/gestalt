package org.terasology.gestalt.di.injection;

public interface Qualifier<T> {

    /**
     * Whether this qualifier contains the given qualifier.
     * @param qualifier The qualifier
     * @return True it does
     */
    default boolean contains(Qualifier<T> qualifier) {
        return equals(qualifier);
    }


}
