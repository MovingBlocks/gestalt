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

package org.terasology.assets.format.producer;

import org.terasology.assets.ResourceUrn;
import org.terasology.naming.Name;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Interface for subscribing for notification of file changes, for either asset files, or delta files.
 * <p>
 * This support asset files (which includes overrides and supplemental files) and delta files
 * @author Immortius
 */
public interface FileChangeSubscriber {

    /**
     * Notification that an asset file was added
     * @param path The path of the asset file
     * @param module The name of the module the file is for
     * @param providingModule The name of the module providing the asset file
     * @return The ResourceUrn of the resource the file contributes too, if any
     */
    Optional<ResourceUrn> assetFileAdded(Path path, Name module, Name providingModule);

    /**
     * Notification that an asset file was modified
     * @param path The path of the asset file
     * @param module The name of the module the file is for
     * @param providingModule The name of the module providing the asset file
     * @return The ResourceUrn of the resource the file contributes too, if any
     */
    Optional<ResourceUrn> assetFileModified(Path path, Name module, Name providingModule);

    /**
     * Notification that an asset file was removed
     * @param path The path of the asset file
     * @param module The name of the module the file is for
     * @param providingModule The name of the module providing the asset file
     * @return The ResourceUrn of the resource the file contributed too, if any
     */
    Optional<ResourceUrn> assetFileDeleted(Path path, Name module, Name providingModule);

    /**
     * Notification that an delta file was added
     * @param path The path of the delta file
     * @param module The name of the module the file is for
     * @param providingModule The name of the module providing the delta file
     * @return The ResourceUrn of the resource the file contributes too, if any
     */
    Optional<ResourceUrn> deltaFileAdded(Path path, Name module, Name providingModule);

    /**
     * Notification that an delta file was modified
     * @param path The path of the delta file
     * @param module The name of the module the file is for
     * @param providingModule The name of the module providing the delta file
     * @return The ResourceUrn of the resource the file contributes too, if any
     */
    Optional<ResourceUrn> deltaFileModified(Path path, Name module, Name providingModule);

    /**
     * Notification that an delta file was removed
     * @param path The path of the delta file
     * @param module The name of the module the file is for
     * @param providingModule The name of the module providing the delta file
     * @return The ResourceUrn of the resource the file contributed too, if any
     */
    Optional<ResourceUrn> deltaFileDeleted(Path path, Name module, Name providingModule);
}
