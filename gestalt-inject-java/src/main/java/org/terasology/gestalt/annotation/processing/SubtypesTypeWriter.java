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

public class SubtypesTypeWriter {
    private static final String META_INF = "META-INF" + File.separator + "subtypes";

    private final Filer filer;
    private final Map<String, HashSet<String>> results = new HashMap<>();
    private final Map<String, FileObject> files = new HashMap<>();

    public SubtypesTypeWriter(Filer filer) {
        this.filer = filer;
    }

    public void writeSubType(String type, String subtype) {
        results.putIfAbsent(type, new HashSet<>());
        results.get(type).add(subtype);
    }

    public void finish() throws IOException {
        for (Map.Entry<String, HashSet<String>> pair : results.entrySet()) {
            FileObject fileObject = filer.createResource(StandardLocation.CLASS_OUTPUT, "", META_INF + File.separator + pair.getKey());
            try (BufferedWriter writer = new BufferedWriter(fileObject.openWriter())) {
                for (String clazz : pair.getValue()) {
                    writer.write(clazz);
                    writer.newLine();
                }
            }
        }
    }
}
