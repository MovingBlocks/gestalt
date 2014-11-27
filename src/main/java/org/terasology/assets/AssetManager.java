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

import com.google.common.collect.Maps;
import org.terasology.module.ModuleEnvironment;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * @author Immortius
 */
public class AssetManager {

    private Map<AssetType, AssetTypeInfo> assetTypeInfoLookup = Maps.newHashMap();
    private Map<String, AssetType> assetTypeLookup = Maps.newHashMap();
    private ModuleEnvironment environment;

    /**
     * Registers an asset type
     *
     * @param type          The type to register
     * @param subfolderName The module asset subfolder to associate with this type.
     * @return Whether the type was added. If a type with same id has already been registered, registration will fail.
     */
    public boolean registerAssetType(AssetType type, String subfolderName) {
        if (assetTypeLookup.containsKey(type.getId())) {
            return false;
        }
        assetTypeLookup.put(type.getId(), type);
        assetTypeInfoLookup.put(type, new AssetTypeInfo(type, subfolderName));
        return true;
    }

    public void registerAssetFormat(AssetType type, AssetFormat format) {

    }

    /**
     * Unregisters an asset type. All loaded assets of this type are disposed.
     *
     * @param type The type to unregister
     */
    public void unregister(AssetType type) {
        if (assetTypeInfoLookup.remove(type) != null) {
            assetTypeLookup.remove(type.getId());
        }
    }

    /**
     * @return An unmodifiable collection of registered asset types
     */
    public Collection<AssetType> getAssetTypes() {
        return Collections.unmodifiableSet(assetTypeInfoLookup.keySet());
    }

    /**
     * Sets the module environment supplying assets.
     * <p>
     * When the module environment changes:
     * <ul>
     *     <li>All assets from modules that are not part of the new environment are disposed</li>
     *     <li>All assets from modules that are part of the new environment are reloaded or disposed, depending on whether they can be found in the new environment</li>
     *     <li>All future asset resolution and loading uses the new environment</li>
     * </ul>
     * @param environment
     */
    public void setEnvironment(ModuleEnvironment environment) {
        this.environment = environment;
    }

    // TODO:
    // * Asset Formats and Factories
    // * Set environment, unloading and reloading assets
    // * Discover assets
    // * Load assets
    // * Create assets
    // * Resolve partial urns
    // * Thread-bound asset loading
    // *

}
