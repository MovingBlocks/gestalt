/*
 * Copyright 2017 MovingBlocks
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
package org.terasology.assets.module;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import org.terasology.naming.Name;

import java.nio.file.Path;
import java.util.Collection;

/**
 * Keeps a cache of paths to asset files for modules, like a flattened virtual "file tree" (map).
 * <p>
 * This speeds up loading assets from modules by only having a module's asset file tree be traversed once on disk (per asset type / folder query),
 * after the first traversal, a list of asset paths from this cache is used instead of traversing the file tree.
 * <p>
 * Each cached module keeps a Map of (AssetTypes+Folders).hashCode() to the list of Paths to those asset types in the given folders.
 */
public class ModuleAssetPathCache {

    /**
     * Cache (map) of ModuleId.toLowerCase() to its map of (assetTypes+folders).hashCode() to asset path list.
     */
    private final LoadingCache<String, ListMultimap<Integer, Path>> cache;

    ModuleAssetPathCache() {
        cache = CacheBuilder.newBuilder()
                .maximumSize(128)
                .build(
                        new CacheLoader<String, ListMultimap<Integer, Path>>() {
                            public ListMultimap<Integer, Path> load(String key) throws Exception {
                                return LinkedListMultimap.create();
                            }
                        }
                );
    }

    /**
     * Adds an asset path to a module's asset path cache.
     *
     * @param module     The module this asset belongs to.
     * @param file       The path to the asset.
     * @param assetTypes A hashcode of the (asset types + folders) currently being loaded.
     */
    public void add(Name module, Path file, int assetTypes) {
        cache.getUnchecked(module.toLowerCase()).put(assetTypes, file);
    }

    /**
     * Gets a list of cached paths for the requested asset types from the requested module.
     *
     * @param moduleId   The module to get assets for.
     * @param assetTypes A hashcode of the (asset types + folders) currently being loaded.
     * @return Filtered stream of paths to be loaded. Null if the requested module or asset types have not been cached (loaded before).
     */
    public Collection<Path> get(Name moduleId, int assetTypes) {
        return cache.getUnchecked(moduleId.toLowerCase()).get(assetTypes);
    }
}
