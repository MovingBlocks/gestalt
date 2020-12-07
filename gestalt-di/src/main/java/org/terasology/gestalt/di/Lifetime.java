package org.terasology.gestalt.di;

public enum Lifetime {
    Scoped,
    ScopedOnlyToChildren,
    Singleton,
    Transient
}
