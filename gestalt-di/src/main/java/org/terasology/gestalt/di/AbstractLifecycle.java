package org.terasology.gestalt.di;

public abstract class AbstractLifecycle implements Lifecycle {
    private final AbstractLifecycle parent;
    public AbstractLifecycle(AbstractLifecycle parent) {
        this.parent = parent;
    }

    public AbstractLifecycle parent() {
        return this.parent;
    }

}
