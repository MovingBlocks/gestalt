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

package org.terasology.module.resources;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * A handle describing and for accessing a
 */
public interface ModuleFile {

    /**
     * @return The name of the file
     */
    String getName();

    /**
     * @return The path to the file
     */
    List<String> getPath();

    /**
     * @return A stream for reading the file. Closing the stream is the duty of the caller
     * @throws IOException If there is an exception opening the file
     */
    InputStream open() throws IOException;

}
