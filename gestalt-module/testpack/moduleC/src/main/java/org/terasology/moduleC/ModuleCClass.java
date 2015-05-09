/*
 * Copyright 2015 MovingBlocks
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

package org.terasology.moduleC;

import org.terasology.test.api.IndexForTest;

import java.nio.file.Files;

/**
 * @author Immortius
 */
@IndexForTest
public class ModuleCClass {

    public void standardMethod() {
        float a = 10;
        float b = 22;
        String result = String.format("%f + %f", a, b);
    }

    public void requiresIoMethod() throws Exception {
        Files.createTempFile("Temp", "txt");
    }
}
