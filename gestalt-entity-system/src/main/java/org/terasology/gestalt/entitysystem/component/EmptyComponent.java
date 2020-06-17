package org.terasology.gestalt.entitysystem.component;

/**
 * An abstract component to be extended if a component that has no information to copy, like components that are flags
 * for properties.
 * <p>
 * This should only be used for components that contain no information. If this is used for a component that does
 * contain data, that data could be lost.
 */
public abstract class EmptyComponent implements Component<EmptyComponent> {

    @Override
    public void copy(EmptyComponent other) {

    }
}
