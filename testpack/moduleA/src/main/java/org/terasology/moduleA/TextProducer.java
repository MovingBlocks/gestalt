package org.terasology.moduleA;

import org.terasology.test.api.ApiInterface;

public class TextProducer implements ApiInterface {
    @Override
    public String apiMethod() {
        return "Hello from a JAR";
    }
}
