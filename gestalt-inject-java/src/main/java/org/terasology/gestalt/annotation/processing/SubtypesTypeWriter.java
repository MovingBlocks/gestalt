// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.gestalt.annotation.processing;

import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Class to write subtypes to disk.
 */
public class SubtypesTypeWriter {
    private static final String META_INF = "META-INF/subtypes";

    private final Filer filer;
    private final Map<String, HashSet<String>> results = new HashMap<>();
    private final Map<String, FileObject> files = new HashMap<>();

    /**
     * Creates a SubtypesTypeWriter to write a subtype to a file.
     * @param filer support creating files by annotation processor.
     */
    public SubtypesTypeWriter(Filer filer) {
        this.filer = filer;
    }

    /**
     * Creates a SubtypesTypeWriter to write a subtype to a file.
     * @param type  the type.
     * @param subtype   the subtype.
     */
    public void writeSubType(String type, String subtype) {
        results.putIfAbsent(type, new HashSet<>());
        results.get(type).add(subtype);
    }

    /**
     * Writes the subtype to file.
     * @throws IOException exception in case writing fails.
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
