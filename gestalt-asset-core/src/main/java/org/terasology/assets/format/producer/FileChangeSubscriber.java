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
 * @author Immortius
 */
public interface FileChangeSubscriber {

    Optional<ResourceUrn> assetFileAdded(Path path, Name module, Name providingModule);

    Optional<ResourceUrn> assetFileModified(Path path, Name module, Name providingModule);

    Optional<ResourceUrn> assetFileDeleted(Path path, Name module, Name providingModule);

    Optional<ResourceUrn> deltaFileAdded(Path path, Name module, Name providingModule);

    Optional<ResourceUrn> deltaFileModified(Path path, Name module, Name providingModule);

    Optional<ResourceUrn> deltaFileDeleted(Path path, Name module, Name providingModule);
}
