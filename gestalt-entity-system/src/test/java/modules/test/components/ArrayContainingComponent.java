package modules.test.components;

import org.terasology.gestalt.entitysystem.component.Component;

import java.util.ArrayList;
import java.util.List;

public class ArrayContainingComponent implements Component<ArrayContainingComponent> {

    public List<String> strings = new ArrayList<>();

    @Override
    public void copyFrom(ArrayContainingComponent other) {
        strings.clear();
        strings.addAll(other.strings);
    }
}
