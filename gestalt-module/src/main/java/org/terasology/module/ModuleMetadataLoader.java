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

package org.terasology.module;

import java.io.IOException;
import java.io.Reader;

/**
 * A module metadata loader reads module metadata from a file.
 */
public interface ModuleMetadataLoader {

    /**
     * @param reader Metadata to load
     * @return The loaded module metadata.
     * @throws IOException If there was an error reading the ModuleMetadata
     */
    ModuleMetadata read(Reader reader) throws IOException;

}
