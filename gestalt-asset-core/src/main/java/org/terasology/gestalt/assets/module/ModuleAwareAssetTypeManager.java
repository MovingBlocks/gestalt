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

package org.terasology.gestalt.assets.module;

import org.terasology.gestalt.assets.Asset;
import org.terasology.gestalt.assets.AssetData;
import org.terasology.gestalt.assets.AssetFactory;
import org.terasology.gestalt.assets.AssetType;
import org.terasology.gestalt.assets.format.producer.AssetFileDataProducer;
import org.terasology.gestalt.assets.management.AssetManager;
import org.terasology.gestalt.assets.management.AssetTypeManager;
import org.terasology.gestalt.module.ModuleEnvironment;

import java.io.Closeable;
import java.util.Collection;

/**
 * ModuleAwareAssetTypeManager is an AssetTypeManager that integrates with a ModuleEnvironment obtaining assets, registering extension classes and handling asset
 * disposal and reloading when environments change.
 * <p>
 * The major features of ModuleAwareAssetTypeManager are:
 * </p>
 * <ul>
 * <li>Registration of core AssetTypes, AssetDataProducers and file formats. These will remain across environment changes.</li>
 * <li>Automatic registration of extension AssetTypes, AssetDataProducers and file formats mark with annotations that are discovered within the module environment
 * being switched to, and removal of these extensions when the module environment is later unloaded</li>
 * <li>Optionally reload all assets from their modules - this is recommended after an environment switch to prevent changes to assets in a previous environment from persisting</li>
 * </ul>
 *
 * @author Immortius
 */
public interface ModuleAwareAssetTypeManager extends Closeable, AssetTypeManager {

    /**
     * Creates and registers an asset type
     *
     * @param type           The type of asset the AssetType will handle
     * @param factory        The factory for creating an asset from asset data
     * @param subfolderNames The names of the subfolders providing asset files, if any
     * @param <T>            The type of Asset
     * @param <U>            The type of AssetData
     * @return The new AssetType
     */
    <T extends Asset<U>, U extends AssetData> AssetType<T, U> createAssetType(Class<T> type, AssetFactory<T, U> factory, String... subfolderNames);

    /**
     * Creates and registers an asset type
     *
     * @param type           The type of asset the AssetType will handle
     * @param factory        The factory for creating an asset from asset data
     * @param subfolderNames The names of the subfolders providing asset files, if any
     * @param <T>            The type of Asset
     * @param <U>            The type of AssetData
     * @return The new AssetType
     */
    <T extends Asset<U>, U extends AssetData> AssetType<T, U> createAssetType(Class<T> type, AssetFactory<T, U> factory, Collection<String> subfolderNames);

    /**
     * Registers an asset type
     *
     * @param assetType      The AssetType to register
     * @param subfolderNames The names of the subfolders providing asset files, if any
     * @param <T>            The type of Asset
     * @param <U>            The type of AssetData
     * @return The new AssetType
     */
    <T extends Asset<U>, U extends AssetData> AssetType<T, U> addAssetType(AssetType<T, U> assetType, String... subfolderNames);

    /**
     * Registers an asset type
     *
     * @param assetType      The AssetType to register
     * @param subfolderNames The names of the subfolders providing asset files, if any
     * @param <T>            The type of Asset
     * @param <U>            The type of AssetData
     * @return The new AssetType
     */
    <T extends Asset<U>, U extends AssetData> AssetType<T, U> addAssetType(AssetType<T, U> assetType, Collection<String> subfolderNames);


    /**
     * @param assetType The AssetType to get the AssetFileDataProducer for. This must be an AssetType handled by this AssetTypeManager
     * @param <T>       The type of Asset handled by the AssetType
     * @param <U>       The type of AssetData handled by the AssetType
     * @return The AssetFileDataProducer for the given AssetType.
     */
    <T extends Asset<U>, U extends AssetData> AssetFileDataProducer<U> getAssetFileDataProducer(AssetType<T, U> assetType);

    /**
     * Removes and closes an asset type.
     *
     * @param type The type of asset to remove
     * @param <T>  The type of asset
     * @param <U>  The type of asset data
     */
    @SuppressWarnings("unchecked")
    <T extends Asset<U>, U extends AssetData> void removeAssetType(Class<T> type);

    /**
     * @return An asset manager over this AssetTypeManager.
     */
    AssetManager getAssetManager();

    /**
     * Switches the module environment. This:
     * <ul>
     * <li>Unloads any previously loaded environment</li>
     * <li>Adds any extension AssetTypes, Formats and Producers</li>
     * <li>Registers all the asset files in the new environment</li>
     * </ul>
     *
     * @param newEnvironment The new module environment
     */
    void switchEnvironment(ModuleEnvironment newEnvironment);

    /**
     * Unloads the current module environment, if any. This:
     * <ul>
     * <li>Removes any extension AssetTypes, Formats and Producers</li>
     * <li>Clears all asset files from the old environment</li>
     * </ul>
     */
    void unloadEnvironment();

    /**
     * Reloads all assets.
     */
    void reloadAssets();

    /**
     * Clears any cache of available assets
     */
    void clearAvailableAssetCache();

}
