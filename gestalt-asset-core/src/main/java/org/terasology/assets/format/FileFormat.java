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

package org.terasology.assets.format;

import org.terasology.assets.exceptions.InvalidAssetFilenameException;
import org.terasology.module.resources.ModuleFile;
import org.terasology.naming.Name;

import java.nio.file.PathMatcher;
import java.util.function.Predicate;

/**
 * Common base interface for all file formats.  A file format is used to load one or more files and either create or modify an
 * {@link org.terasology.assets.AssetData AssetData}.
 * @author Immortius
 */
public interface FileFormat {
    /**
     * @return A path matcher that will filter for files relevant for this format.
     */
    Predicate<ModuleFile> getFileMatcher();

    /**
     * This method is use to obtain the name of the resource represented by the given filename. The ModuleAssetDataProducer will combine it with a module id to
     * determine the complete ResourceUrn.
     * @param filename The filename of an asset, including extension
     * @return The asset name corresponding to the given filename
     * @throws org.terasology.assets.exceptions.InvalidAssetFilenameException if the filename is not valid for this format.
     */
    Name getAssetName(String filename) throws InvalidAssetFilenameException;
}
