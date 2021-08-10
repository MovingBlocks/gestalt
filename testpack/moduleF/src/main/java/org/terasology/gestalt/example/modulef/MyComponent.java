package org.terasology.gestalt.example.modulef;

import org.terasology.gestalt.entitysystem.component.Component;

public class MyComponent implements Component<MyComponent> {
    private String name = "";

    public MyComponent() {

    }

    public MyComponent(MyComponent other) {
        this.name = other.name;
    }

    @Override
    public void copyFrom(MyComponent other) {
        this.name = other.name;
    }
}
