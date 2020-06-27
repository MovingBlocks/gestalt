package org.terasology.gestalt.entitysystem.component;

/**
 * An abstract component that can be extended from for any component with no mutable elements - so components with no attributes. This avoids needing to implement the copy method.
 * EmptyComponent may be used for flagging behavior where no configuration is required. I would suggest this should not come up hugely often, and if you are finding you have a lot
 * of empty components you might consider whether they would be better handled by another component having boolean attributes.
 * <p>
 * Note if this is used for a component with attributes and the copy method is not overwritten then the component will not behave as desired - effectively the entity system will
 * not retain any values given to the attributes.
 */
public abstract class EmptyComponent implements Component<EmptyComponent> {

    @Override
    public void copy(EmptyComponent other) {

    }
}
