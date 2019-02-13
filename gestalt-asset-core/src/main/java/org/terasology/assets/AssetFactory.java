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

package org.terasology.assets;

import org.terasology.module.sandbox.API;

/**
 * AssetFactorys are used to load AssetData into new assets.
 * <p>For many assets, the assets just have one asset implementation so the factory would simply call the constructor for the implementation and pass the urn and data
 * straight through. However other assets may have multiple implementations (e.g. Texture may have an OpenGL and a DirectX implementation) so the factory installed
 * will determine that. Additionally the factory may pass through other information (OpenGL texture handle, or a reference to a central OpenGL context).</p>
 *
 * @author Immortius
 */
@API
@FunctionalInterface
public interface AssetFactory<T extends Asset<U>, U extends AssetData> {

    /**
     * @param urn       The urn of the asset to construct
     * @param assetType The assetType the asset belongs to
     * @param data      The data for the asset
     * @return The built asset
     * @throws org.terasology.assets.exceptions.InvalidAssetDataException If the asset failed to load due to invalid data
     */
    T build(ResourceUrn urn, AssetType<T, U> assetType, U data);
}
