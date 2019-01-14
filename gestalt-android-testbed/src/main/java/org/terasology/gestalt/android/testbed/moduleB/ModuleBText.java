package org.terasology.gestalt.android.testbed.moduleB;

import org.terasology.gestalt.android.testbed.engine.TextProducer;

public class ModuleBText implements TextProducer {
    @Override
    public String getText() {
        return "Greetings from Module B";
    }
}
