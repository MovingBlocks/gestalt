/*
 * Copyright 2015 MovingBlocks
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
import org.terasology.module.filesystem.ModuleFileSystemProvider;
import org.terasology.naming.Name;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

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

    private Cache<Module, PathCache> assetPathCache;
    private Cache<Module, PathCache> overridePathCache;
    private Cache<Module, PathCache> deltaPathCache;

    public ModuleAssetScanner() {
        this(128);
    }

    public ModuleAssetScanner(int cacheSize) {
        assetPathCache = CacheBuilder.<Module, PathCache>newBuilder()
                .maximumSize(cacheSize)
                .removalListener(notification -> logger.debug("Cleared cache of asset paths for {} - {}", notification.getKey(), notification.getCause()))
                .build();
        overridePathCache = CacheBuilder.<Module, PathCache>newBuilder()
                .maximumSize(cacheSize)
                .removalListener(notification -> logger.debug("Cleared cache of override paths for {} - {}", notification.getKey(), notification.getCause()))
                .build();
        deltaPathCache = CacheBuilder.<Module, PathCache>newBuilder()
                .maximumSize(cacheSize)
                .removalListener(notification -> logger.debug("Cleared cache of delta paths for {} - {}", notification.getKey(), notification.getCause()))
                .build();
    }

    public void scan(ModuleEnvironment environment, AssetFileDataProducer<?> producer) {
        scanForAssets(environment, producer);
        scanForOverrides(environment, producer);
        scanForDeltas(environment, producer);
    }
    
    private void scanForAssets(ModuleEnvironment environment, AssetFileDataProducer<?> producer) {
        for (Module module : environment.getModulesOrderedByDependencies()) {
            try {
                PathCache pathCache = assetPathCache.get(module, () -> {
                    PathCache newCache = new PathCache();
                    scanForPathCache(environment.getFileSystem().getPath(ModuleFileSystemProvider.ROOT, module.getId().toString(), ASSET_FOLDER), newCache);
                    return newCache;
                });

                for (String folderName : producer.getFolderNames()) {
                    for (Path path : pathCache.getPathsByRootFolder().get(new Name(folderName)).stream().map(t -> environment.getFileSystem().getPath(t)).collect(Collectors.toList())) {
                        producer.assetFileAdded(path, module.getId(), module.getId());
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
                PathCache pathCache = overridePathCache.get(module, () -> {
                    PathCache newCache = new PathCache();
                    Path overridePath = environment.getFileSystem().getPath(ModuleFileSystemProvider.ROOT, module.getId().toString(), OVERRIDE_FOLDER);
                    if (Files.isDirectory(overridePath)) {
                        try (DirectoryStream<Path> moduleOverrides = Files.newDirectoryStream(overridePath)) {
                            for (Path moduleDir : moduleOverrides) {
                                scanForPathCache(moduleDir, newCache);
                            }
                        }
                    }
                    return newCache;
                });

                for (String folderName : producer.getFolderNames()) {
                    for (Path path : pathCache.getPathsByRootFolder().get(new Name(folderName)).stream().map(t -> environment.getFileSystem().getPath(t)).collect(Collectors.toList())) {
                        producer.assetFileAdded(path, new Name(path.getName(2).toString()), module.getId());
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
                PathCache pathCache = deltaPathCache.get(module, () -> {
                    PathCache newCache = new PathCache();
                    Path deltaPath = environment.getFileSystem().getPath(ModuleFileSystemProvider.ROOT, module.getId().toString(), DELTA_FOLDER);
                    if (Files.isDirectory(deltaPath)) {
                        try (DirectoryStream<Path> moduleOverrides = Files.newDirectoryStream(deltaPath)) {
                            for (Path moduleDir : moduleOverrides) {
                                scanForPathCache(moduleDir, newCache);
                            }
                        }
                    }
                    return newCache;
                });

                for (String folderName : producer.getFolderNames()) {
                    for (Path path : pathCache.getPathsByRootFolder().get(new Name(folderName)).stream().map(t -> environment.getFileSystem().getPath(t)).collect(Collectors.toList())) {
                        producer.deltaFileAdded(path, new Name(path.getName(2).toString()), module.getId());
                    }
                }
            } catch (ExecutionException e) {
                logger.error("Failed to scan delta path", e);
            }
        }
    }

    private void scanForPathCache(Path rootPath, PathCache cache) {
        if (Files.exists(rootPath)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(rootPath)) {
                for (Path subpath : stream) {
                    Name folderName = new Name(subpath.getFileName().toString());
                    if (Files.isDirectory(subpath)) {
                        Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                cache.getPathsByRootFolder().put(folderName, file.toString());
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    }
                }
            } catch (IOException e) {
                logger.error("Failed to scan module path {}", rootPath, e);
            }
        }
    }

    private static class PathCache {
        private ListMultimap<Name, String> pathsByFolder = ArrayListMultimap.create();

        ListMultimap<Name, String> getPathsByRootFolder() {
            return pathsByFolder;
        }
    }

}
