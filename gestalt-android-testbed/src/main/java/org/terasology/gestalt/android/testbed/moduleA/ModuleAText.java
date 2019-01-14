package org.terasology.gestalt.android.testbed.moduleA;

import org.terasology.gestalt.android.testbed.engine.TextProducer;

public class ModuleAText implements TextProducer {
    @Override
    public String getText() {
        return "Hello from Module A!";
    }
}
