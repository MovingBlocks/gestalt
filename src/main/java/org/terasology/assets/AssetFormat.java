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

package org.terasology.assets;

import org.terasology.assets.exceptions.InvalidAssetFilenameException;
import org.terasology.naming.Name;
import org.terasology.naming.ResourceUrn;

import java.io.IOException;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.Set;

/**
 * @author Immortius
 */
public interface AssetFormat<T extends AssetData> {

    /**
     * @return A path matcher that will filter for files relevant for this format.
     */
    PathMatcher getFileMatcher();

    /**
     * @param filename The filename of an asset, including extension
     * @return The asset name corresponding to the given filename
     * @throws InvalidAssetFilenameException if the filename is not valid for this format.
     */
    Name getAssetName(String filename) throws InvalidAssetFilenameException;

    /**
     * Loads an asset.
     * @param urn The urn identifying the asset being loaded.
     * @param inputs The inputs corresponding to this asset
     * @return The loaded asset
     * @throws IOException If there are any errors loading the asset
     */
    T load(ResourceUrn urn, List<AssetInput> inputs) throws IOException;
}
