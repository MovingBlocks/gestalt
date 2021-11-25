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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.gestalt.di.index.CompoundClassIndex;
import org.terasology.gestalt.di.index.UrlClassIndex;
import org.terasology.gestalt.module.Module;
import org.terasology.gestalt.module.ModuleMetadata;
import org.terasology.gestalt.module.ModuleMetadataJsonAdapter;
import org.terasology.gestalt.module.ModulePathScanner;
import org.terasology.gestalt.module.ModuleRegistry;

import java.io.BufferedReader;
import java.io.File;
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
 *
 * For code modules, the dexes/jars are copied into app storage but everything else is read directly from the APK.
 */
public class AndroidModulePathScanner extends ModulePathScanner {
    private static final Logger logger = LoggerFactory.getLogger(AndroidModulePathScanner.class);
    private final AssetManager assetManager;
    private final File moduleCodeCache;

    public AndroidModulePathScanner(AssetManager assetManager, File moduleCodeCache) {
        super(null);
        this.assetManager = assetManager;
        this.moduleCodeCache = moduleCodeCache;
    }

    private void deleteModuleCodeCache(File file) {
        if (file.isDirectory()) {
            for (String subPath : file.list()) {
                deleteModuleCodeCache(new File(file, subPath));
            }
        }
        file.delete();
    }

    @Override
    public void scan(ModuleRegistry registry, Collection<File> paths) {
        if (moduleCodeCache.exists()) {
            deleteModuleCodeCache(moduleCodeCache);
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

                    CompoundClassIndex classIndex = new CompoundClassIndex();

                    List<File> classpaths = new ArrayList<>();
                    try {
                        for (String moduleCode : assetManager.list(fullModulePath + "/build/dexes")) {
                            if (moduleCode.endsWith(".dex") || moduleCode.endsWith(".jar")) {
                                new File(moduleCodeCache, modulePath).mkdirs();
                                File cachedModuleCode = new File(moduleCodeCache, modulePath + "/" + moduleCode);
                                try (InputStream inputStream = assetManager.open(fullModulePath + "/build/dexes/" + moduleCode)) {
                                    try (FileOutputStream outputStream = new FileOutputStream(cachedModuleCode)) {
                                        ByteStreams.copy(inputStream, outputStream);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                classpaths.add(cachedModuleCode);
                                if (moduleCode.endsWith(".jar")) {
                                    classIndex.add(UrlClassIndex.byArchive(cachedModuleCode));
                                } else {
                                    logger.warn("Using raw .dex files means that gestalt-di will not work." +
                                                "Try combining them into a jar containing the META-INF directory.");
                                }
                            }
                        }
                    } catch (Exception ignore) {
                    }

                    registry.add(new Module(metadata,
                            new AndroidAssetsFileSource(assetManager, fullModulePath),
                            classpaths, classIndex, x -> true));
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
