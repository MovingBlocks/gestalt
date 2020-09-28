package org.terasology.gestalt.di;

public class CompositeLifecycle {
    private final AbstractLifecycle[] lifecycles;

    public CompositeLifecycle(AbstractLifecycle ... lifecycles) {
        this.lifecycles = lifecycles;
    }

}
