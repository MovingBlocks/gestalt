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

public class ServiceTypeWriter {
    private static final String META_INF = "META-INF" + File.separator + "services";

    private final Filer filer;
    private final Map<String, HashSet<String>> results = new HashMap<>();
    private final Map<String, FileObject> files = new HashMap<>();

    public ServiceTypeWriter(Filer filer) {
        this.filer = filer;
    }

    public void writeService(String service, String target) {
        results.putIfAbsent(service, new HashSet<>());
        results.get(service).add(target);
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
