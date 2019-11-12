/*
 * Copyright 2019 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terasology.gestalt.module.resources;

import org.junit.BeforeClass;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;

public class ClasspathFileSourceTest extends BaseFileSourceTest {

    private static ClasspathFileSource source;

    @BeforeClass
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
