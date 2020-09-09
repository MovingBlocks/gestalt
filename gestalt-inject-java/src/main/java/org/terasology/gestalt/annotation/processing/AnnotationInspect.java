package org.terasology.gestalt.annotation.processing;

import javax.annotation.processing.ProcessingEnvironment;

public class AnnotationInspect {
    private final ProcessingEnvironment environment;

    public AnnotationInspect(ProcessingEnvironment environment){
        this.environment = environment;
    }
}
