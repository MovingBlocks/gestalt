/*
 * Copyright 2021 The Terasology Foundation
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

package org.terasology.gestalt.android;

import android.content.res.AssetManager;
import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.gestalt.module.Module;
import org.terasology.gestalt.module.ModuleMetadata;
import org.terasology.gestalt.module.ModuleMetadataJsonAdapter;
import org.terasology.gestalt.module.ModulePathScanner;
import org.terasology.gestalt.module.ModuleRegistry;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A {@link ModulePathScanner} derivative that scans the APK assets directory on Android for modules.
 * It uses {@link AndroidAssetsFileSource} to create modules that directly use these files, removing the need to copy
 * the modules into the app's data directory first.
 */
public class AndroidModulePathScanner extends ModulePathScanner {
    private static final Logger logger = LoggerFactory.getLogger(AndroidModulePathScanner.class);
    private final AssetManager assetManager;
    private final File moduleDexesCache;

    public AndroidModulePathScanner(AssetManager assetManager, File moduleDexesCache) {
        super();
        this.assetManager = assetManager;
        this.moduleDexesCache = moduleDexesCache;
    }

    private void deleteModuleDexesCache(File file) {
        if (file.isDirectory()) {
            for (String subPath : file.list()) {
                deleteModuleDexesCache(new File(file, subPath));
            }
        }
        file.delete();
    }

    @Override
    public void scan(ModuleRegistry registry, Collection<File> paths) {
        if (moduleDexesCache.exists()) {
            deleteModuleDexesCache(moduleDexesCache);
        }

        for (File path : paths) {
            try {
                for (String modulePath : assetManager.list(path.toString())) {
                    String fullModulePath = path + "/" + modulePath;
                    InputStream modInfoFile = tryOpenAsset(fullModulePath + "/module.json");
                    if (modInfoFile == null) {
                        logger.warn("Found a possible module without a module.json. Skipping...");
                        continue;
                    }

                    ModuleMetadata metadata;
                    try (Reader reader = new BufferedReader(new InputStreamReader(modInfoFile, Charsets.UTF_8))) {
                        metadata = new ModuleMetadataJsonAdapter().read(reader);
                    } catch (Exception e) {
                        logger.error("Error reading module metadata", e);
                        continue;
                    }

                    Reflections reflections = new Reflections();
                    try {
                        InputStream reflectionsFile = assetManager.open(fullModulePath + "/build/classes/reflections.cache");
                        reflections.collect(reflectionsFile);
                    } catch (FileNotFoundException ignore) {
                        logger.warn("No reflections cache found for module. Is it an asset-only module?");
                    } catch (Exception e) {
                        logger.error("Error reading reflections", e);
                    }

                    List<File> classpaths = new ArrayList<>();
                    try {
                        for (String moduleDex : assetManager.list(fullModulePath + "/build/dexes")) {
                            if (moduleDex.endsWith(".dex")) {
                                new File(moduleDexesCache, modulePath).mkdirs();
                                File cachedModuleDex = new File(moduleDexesCache, modulePath + "/" + moduleDex);
                                try (InputStream inputStream = assetManager.open(fullModulePath + "/build/dexes/" + moduleDex)) {
                                    try (FileOutputStream outputStream = new FileOutputStream(cachedModuleDex)) {
                                        ByteStreams.copy(inputStream, outputStream);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                classpaths.add(cachedModuleDex);
                            }
                        }
                    } catch (Exception ignore) {
                    }

                    registry.add(new Module(metadata,
                            new AndroidAssetsFileSource(assetManager, fullModulePath),
                            classpaths, reflections, x -> true));
                }
            } catch (Exception ignore) {
            }
        }
    }

    private InputStream tryOpenAsset(String path) {
        try {
            return assetManager.open(path);
        } catch (IOException ignore) {
            return null;
        }
    }
}
