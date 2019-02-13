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

import java.io.IOException;

/**
 * An AssetAlterationFileFormat handles loading a file providing additional information or modifications to an asset
 * and applying it to an appropriate {@link org.terasology.assets.AssetData AssetData}.
 * @author Immortius
 */
public interface AssetAlterationFileFormat<T extends AssetData> extends FileFormat {

    /**
     * Applies an alteration to the given assetData
     *
     * @param input     The input corresponding to this asset
     * @param assetData An assetData to update
     * @throws java.io.IOException If there are any errors loading the alteration
     */
    void apply(AssetDataFile input, T assetData) throws IOException;
}
