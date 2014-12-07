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

    private Map<String, AssetType<?, ?>> assetTypeLookup = Maps.newHashMap();
    private Map<Class<? extends Asset<?>>, AssetType> assetClassToTypeLookup = Maps.newHashMap();
    private ModuleEnvironment environment;

    public AssetType getAssetType(String id) {
        return assetTypeLookup.get(id);
    }

    public <T extends Asset<U>, U extends AssetData> AssetType<T, U> getAssetType(Class<? extends T> assetClass) {
        return assetClassToTypeLookup.get(assetClass);
    }

    /**
     * @return An unmodifiable collection of registered asset types
     */
    public Collection<AssetType<?, ?>> getAssetTypes() {
        return Collections.unmodifiableCollection(assetTypeLookup.values());
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
