package org.terasology.context;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnnotationMetadataWrapper implements AnnotationMetadata {
    Map<String, Object> fields;
    Map<String, AnnotationMetadataWrapper> annotations;

    public AnnotationMetadataWrapper(Map<String, Object> fields, Map<String, AnnotationMetadataWrapper> annotation) {
        this.fields = fields == null ? new HashMap<>() : fields;
        this.annotations = annotation == null ? new HashMap<>() : annotation;
    }

    public static AnnotationMetadataWrapper Build(Map<String, Object> fields, Map<String, AnnotationMetadataWrapper> annotation) {
        return new AnnotationMetadataWrapper(fields, annotation);
    }

    @Override
    public <T> T getField(Class<? extends Annotation> ann, String field, Class<T> targetType) {
        return null;
    }

    public List<AnnotationMetadata> getAnnotationsByStereotype(String stereotype) {
        List<AnnotationMetadata> result = new ArrayList<>(10);
        if (annotations.containsKey(stereotype)) {
            result.add(this);
        }
        for (AnnotationMetadataWrapper wrapper : annotations.values()) {
            wrapper.internalGetAnnotationByStereotype(stereotype, result);
        }
        return result;
    }

    public void internalGetAnnotationByStereotype(String stereotype, List<AnnotationMetadata> result) {
        if (annotations.containsKey(stereotype)) {
            result.add(this);
        }
        for (AnnotationMetadataWrapper wrapper : annotations.values()) {
            wrapper.internalGetAnnotationByStereotype(stereotype, result);
        }
    }

    @Override
    public boolean hasAnnotation(Class<? extends Annotation> ann) {
        return annotations.containsKey(ann);
    }

    @Override
    public boolean hasAnnotation(String ann) {
        return false;
    }

    @Override
    public boolean hasStereotype(Class<? extends Annotation> ann) {
        if (annotations.containsKey(ann.getName())) {
            return true;
        }
        for (AnnotationMetadataWrapper wrapper : annotations.values()) {
            if (wrapper.hasStereotype(ann)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasStereotype(String ann) {
        return false;
    }
}
