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

package org.terasology.gestalt.assets.management;

import org.terasology.gestalt.assets.Asset;
import org.terasology.gestalt.assets.AssetData;
import org.terasology.gestalt.assets.AssetType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * AssetTypeManagers manage a set of AssetTypes, allowing them to be retrieved by Asset class.
 *
 * @author Immortius
 */
public interface AssetTypeManager {

    /**
     * Retrieves the AssetType for a given class of Asset, if available.
     *
     * @param type The class of Asset to get the type of
     * @param <T>  The type of Asset
     * @param <U>  The type of AssetData
     * @return The requested AssetType if available
     */
    <T extends Asset<U>, U extends AssetData> Optional<AssetType<T, U>> getAssetType(Class<T> type);

    /**
     * Retrieves the possible AssetTypes for a given class of Asset. This should include subtypes.
     * <p>
     * Example: given AssetB and AssetC which are subtypes of AssetA, getAssetTypes(AssetA.class) should return all of
     * the AssetTypes for AssetA, AssetB and AssetC which are available.
     * </p>
     *
     * @param type The class of Asset to get the AssetTypes for
     * @param <T>  The class of Asset
     * @return A list of available AssetTypes.
     */
    <T extends Asset<?>> List<AssetType<? extends T, ?>> getAssetTypes(Class<T> type);

    /**
     * @return Retrieves a list of all available asset types
     */
    Collection<AssetType<?, ?>> getAssetTypes();

    /**
     * Disposes any assets that are unused (not referenced)
     */
    void disposedUnusedAssets();


}
