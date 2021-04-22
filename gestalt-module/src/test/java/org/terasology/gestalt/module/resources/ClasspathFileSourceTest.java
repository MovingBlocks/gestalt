// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.gestalt.module.resources;

import org.junit.jupiter.api.BeforeAll;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;

public class ClasspathFileSourceTest extends BaseFileSourceTest {

    private static ClasspathFileSource source;

    @BeforeAll
    public static void setup() {
        ResourcesScanner resourcesScanner = new ResourcesScanner();
        resourcesScanner.setResultFilter(x -> false);
        Reflections manifest = new Reflections("content", resourcesScanner);
        source = new ClasspathFileSource(manifest, "content");
    }

    @Override
    public ModuleFileSource getFileSource() {
        return source;
    }
}
