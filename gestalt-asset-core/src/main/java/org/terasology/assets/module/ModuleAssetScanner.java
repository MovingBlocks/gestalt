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

package org.terasology.assets.module;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.assets.format.producer.AssetFileDataProducer;
import org.terasology.module.Module;
import org.terasology.module.ModuleEnvironment;
import org.terasology.module.resources.ModuleFile;
import org.terasology.naming.Name;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * ModuleAssetScanner scans a module environment for all available asset files, notifying relevant AssetFileDataProducers of their existence.
 * <p>
 * ModuleAssetScanner caches its scans to help speed up environment switches. If there are file changes that need to be detected then {@link #clearCache()} should be used
 * to clear the cache prior to scanning.
 */
public class ModuleAssetScanner {

    /**
     * The name of the module directory that contains asset files.
     */
    public static final String ASSET_FOLDER = "assets";

    /**
     * The name of the module directory that contains overrides.
     */
    public static final String OVERRIDE_FOLDER = "overrides";

    /**
     * The name of the module directory that contains detlas.
     */
    public static final String DELTA_FOLDER = "deltas";

    private static final Logger logger = LoggerFactory.getLogger(ModuleAssetScanner.class);

    private Cache<Module, CacheEntry> assetPathCache;
    private Cache<Module, CacheEntry> overridePathCache;
    private Cache<Module, CacheEntry> deltaPathCache;

    /**
     * Creates a ModuleAssetScanner with cacheSize 128
     */
    public ModuleAssetScanner() {
        this(128);
    }

    /**
     * Creates a ModuleAssetScanner
     *
     * @param cacheSize The number of modules to cache the file paths of. When scanning a module beyond this size limit the cache of the least recently used module will be dropped.
     */
    public ModuleAssetScanner(int cacheSize) {
        assetPathCache = CacheBuilder.<Module, CacheEntry>newBuilder()
                .maximumSize(cacheSize)
                .removalListener(notification -> logger.debug("Cleared cache of asset paths for {} - {}", notification.getKey(), notification.getCause()))
                .build();
        overridePathCache = CacheBuilder.<Module, CacheEntry>newBuilder()
                .maximumSize(cacheSize)
                .removalListener(notification -> logger.debug("Cleared cache of override paths for {} - {}", notification.getKey(), notification.getCause()))
                .build();
        deltaPathCache = CacheBuilder.<Module, CacheEntry>newBuilder()
                .maximumSize(cacheSize)
                .removalListener(notification -> logger.debug("Cleared cache of delta paths for {} - {}", notification.getKey(), notification.getCause()))
                .build();
    }

    /**
     * Scans a module environment and adds all asset, override and delta files to the given producer
     *
     * @param environment The environment to scan
     * @param producer    The producer to register available files to
     */
    public void scan(ModuleEnvironment environment, AssetFileDataProducer<?> producer) {
        scanForAssets(environment, producer);
        scanForOverrides(environment, producer);
        scanForDeltas(environment, producer);
    }

    /**
     * Clears any cached path information.
     */
    public void clearCache() {
        assetPathCache.invalidateAll();
        overridePathCache.invalidateAll();
        deltaPathCache.invalidateAll();
    }

    private void scanForAssets(ModuleEnvironment environment, AssetFileDataProducer<?> producer) {
        for (Module module : environment.getModulesOrderedByDependencies()) {
            try {
                CacheEntry cacheEntry = assetPathCache.get(module, () -> {
                    CacheEntry newCache = new CacheEntry();
                    scanForPathCache(module, newCache, ASSET_FOLDER);
                    return newCache;
                });

                for (String folderName : producer.getFolderNames()) {
                    for (ModuleFile file : cacheEntry.getPathsByRootFolder().get(new Name(folderName))) {
                        producer.assetFileAdded(file, module.getId(), module.getId());
                    }
                }
            } catch (ExecutionException e) {
                logger.error("Failed to scan asset path", e);
            }
        }
    }

    private void scanForOverrides(ModuleEnvironment environment, AssetFileDataProducer<?> producer) {
        for (Module module : environment.getModulesOrderedByDependencies()) {
            try {
                CacheEntry cacheEntry = overridePathCache.get(module, () -> {
                    CacheEntry newCache = new CacheEntry();
                    Set<String> subpaths = module.getResources().getSubpaths(OVERRIDE_FOLDER);
                    for (String overrideModule : subpaths) {
                        scanForPathCache(module, newCache, OVERRIDE_FOLDER, overrideModule);
                    }
                    return newCache;
                });

                for (String folderName : producer.getFolderNames()) {
                    for (ModuleFile file : cacheEntry.getPathsByRootFolder().get(new Name(folderName))) {
                        producer.assetFileAdded(file, new Name(file.getPath().get(1)), module.getId());
                    }
                }
            } catch (ExecutionException e) {
                logger.error("Failed to scan asset path", e);
            }
        }
    }

    private void scanForDeltas(ModuleEnvironment environment, AssetFileDataProducer<?> producer) {
        for (Module module : environment.getModulesOrderedByDependencies()) {
            try {
                CacheEntry cacheEntry = deltaPathCache.get(module, () -> {
                    CacheEntry newCache = new CacheEntry();
                    Set<String> subpaths = module.getResources().getSubpaths(DELTA_FOLDER);
                    for (String moduleDelta : subpaths) {
                        scanForPathCache(module, newCache, DELTA_FOLDER, moduleDelta);
                    }
                    return newCache;
                });

                for (String folderName : producer.getFolderNames()) {
                    for (ModuleFile file : cacheEntry.getPathsByRootFolder().get(new Name(folderName))) {
                        producer.deltaFileAdded(file, new Name(file.getPath().get(1)), module.getId());
                    }
                }
            } catch (ExecutionException e) {
                logger.error("Failed to scan delta path", e);
            }
        }
    }

    private void scanForPathCache(Module originModule, CacheEntry cache, String... rootPath) {
        for (String typeFolder : originModule.getResources().getSubpaths(rootPath)) {
            Name type = new Name(typeFolder);
            for (ModuleFile moduleFile : originModule.getResources().getFilesInPath(true, concat(rootPath, typeFolder))) {
                cache.getPathsByRootFolder().put(type, moduleFile);
            }
        }
    }

    private String[] concat(String[] array, String item) {
        String[] result = Arrays.copyOf(array, array.length + 1);
        result[array.length] = item;
        return result;
    }

    private static class CacheEntry {
        private ListMultimap<Name, ModuleFile> pathsByFolder = ArrayListMultimap.create();

        ListMultimap<Name, ModuleFile> getPathsByRootFolder() {
            return pathsByFolder;
        }
    }


}
