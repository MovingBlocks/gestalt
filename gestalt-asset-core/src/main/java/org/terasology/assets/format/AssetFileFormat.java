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

package org.terasology.assets.format;

import org.terasology.assets.AssetData;
import org.terasology.assets.ResourceUrn;

import java.io.IOException;
import java.util.List;

/**
 * An AssetFileFormat handles loading a file representation of an asset into the appropriate {@link org.terasology.assets.AssetData AssetData}.
 * @author Immortius
 */
public interface AssetFileFormat<T extends AssetData> extends FileFormat {

    /**
     * Creates an {@link org.terasology.assets.AssetData AssetData} from one or more files.
     *
     * @param urn    The urn identifying the asset being loaded.
     * @param inputs The inputs corresponding to this asset
     * @return The loaded asset
     * @throws IOException If there are any errors loading the asset
     */
    T load(ResourceUrn urn, List<AssetDataFile> inputs) throws IOException;
}
