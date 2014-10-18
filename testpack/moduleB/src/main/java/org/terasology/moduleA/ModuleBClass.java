/*
 * Copyright 2014 MovingBlocks
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

package org.terasology.moduleA;

import java.nio.file.Files;
import java.util.Comparator;

/**
 * @author Immortius
 */
public class ModuleBClass implements Comparator<String> {

    public void standardMethod() {
        float a = 10;
        float b = 22;
        String result = String.format("%f + %f", a, b);
    }

    public void requiresIoMethod() throws Exception {
        Files.createTempFile("Temp", "txt");
    }

    @Override
    public int compare(String o1, String o2) {
        return 0;
    }
}
