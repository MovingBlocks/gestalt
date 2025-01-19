// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.gestalt.annotation.processing;

import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * The AnnotationTypeWriter class is responsible for writing to files under META-INF/annotations listing which classes
 * are annotated with a particular annotation. For example, types annotated with @IndexInherited would be listed in
 * META-INF/annotations/org.terasology.context.annotation.IndexInherited.
 */
public class AnnotationTypeWriter {
    private static final String META_INF = "META-INF/annotations";

    private final Filer filer;
    private final Map<String, HashSet<String>> results = new HashMap<>();
    private final Map<String, FileObject> files = new HashMap<>();

    /**
     * Creates an AnnotationTypeWriter.
     * @param filer support creating files by annotation processor.
     */
    public AnnotationTypeWriter(Filer filer) {
        this.filer = filer;
    }

    /**
     * Registers that a given class should be associated as being annotated by a given annotation (aka. service).
     * In this instance, a service refers to a particular annotation type. So, the service parameter would be
     *
     * @param service the fully-qualified type name of the annotation.
     * @param target the target class that was annotated with that annotation.
     */
    public void writeAnnotation(String service, String target) {
        results.putIfAbsent(service, new HashSet<>());
        results.get(service).add(target);
    }

    /**
     * Writes out annotation-class mappings to files.
     * @throws IOException exception in case write fails.
     */
    public void finish() throws IOException {
        for (Map.Entry<String, HashSet<String>> pair : results.entrySet()) {
            FileObject fileObject = filer.createResource(StandardLocation.CLASS_OUTPUT, "", META_INF + "/" + pair.getKey());
            try (BufferedWriter writer = new BufferedWriter(fileObject.openWriter())) {
                for (String clazz : pair.getValue()) {
                    writer.write(clazz);
                    writer.newLine();
                }
            }
        }
    }
}
