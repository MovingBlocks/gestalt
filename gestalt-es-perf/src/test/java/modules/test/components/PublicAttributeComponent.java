package modules.test.components;

import org.terasology.gestalt.entitysystem.component.Component;

public class PublicAttributeComponent implements Component<PublicAttributeComponent> {

    public String name = "";

    @Override
    public void copy(PublicAttributeComponent other) {
        this.name = other.name;
    }
}
