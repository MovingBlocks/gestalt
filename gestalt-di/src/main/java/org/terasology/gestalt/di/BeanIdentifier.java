package org.terasology.gestalt.di;

import org.terasology.context.AnnotationMetadata;
import org.terasology.context.AnnotationValue;

public class BeanIdentifier<T> {
    private AnnotationMetadata metadata;
    AnnotationValue target;

    public BeanIdentifier(AnnotationMetadata metadata, String qualifier){
        metadata.getAnnotationsByStereotype(qualifier);
    }

}
