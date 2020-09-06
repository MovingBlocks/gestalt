package org.terasology.context;

import java.util.HashMap;
import java.util.Map;

public class AbstractAnnotationMetadata {

    Map<String, Map<CharSequence, Object>> annotations = new HashMap<>();

    Map<String, Map<CharSequence, Object>> stereotypes = new HashMap<>();

    public AbstractAnnotationMetadata(Map<String, Map<CharSequence, Object>> annotations, Map<String, Map<CharSequence, Object>> stereotypes) {
        this.annotations = annotations;
        this.stereotypes = stereotypes;
    }

}
