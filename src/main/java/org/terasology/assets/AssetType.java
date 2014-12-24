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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.assets.exceptions.InvalidAssetFilenameException;
import org.terasology.module.Module;
import org.terasology.module.ModuleEnvironment;
import org.terasology.naming.Name;
import org.terasology.naming.ResourceUrn;
import org.terasology.util.io.FileScanning;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AssetType<T extends Asset<U>, U extends AssetData> {

    private static final String ASSET_FOLDER = "assets";
    private static final Logger logger = LoggerFactory.getLogger(AssetType.class);

    private final Name id;
    private final String folderName;
    private final Class<T> assetClass;

    private ModuleEnvironment moduleEnvironment;
    private AssetFactory<T, U> factory;
    private List<AssetFormat<U>> formats = Lists.newArrayList();

    private Map<ResourceUrn, T> loadedAssets = Maps.newHashMap();

    private Table<Name, Name, UnloadedAsset<U>> unloadedAssetLookup = HashBasedTable.create();


    public AssetType(String id, String folderName, Class<T> assetClass) {
        this(new Name(id), folderName, assetClass);
    }

    public AssetType(Name id, String folderName, Class<T> assetClass) {
        Preconditions.checkNotNull(id);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(folderName), "folderName must not be null or empty");
        Preconditions.checkNotNull(assetClass);

        this.id = id;
        this.folderName = folderName;
        this.assetClass = assetClass;
    }

    public Name getId() {
        return id;
    }

    public Class<T> getAssetClass() {
        return assetClass;
    }

    public ModuleEnvironment getEnvironment() {
        return moduleEnvironment;
    }

    public void setEnvironment(ModuleEnvironment environment) {
        this.moduleEnvironment = environment;
        unloadedAssetLookup.clear();
        for (AssetFormat<U> format : formats) {
            scanForFormat(format);
        }
    }

    public AssetFactory<T, U> getFactory() {
        return factory;
    }

    public void setFactory(AssetFactory<T, U> factory) {
        this.factory = factory;
        for (T asset : loadedAssets.values()) {
            asset.dispose();
        }
        loadedAssets.clear();
    }

    public List<AssetFormat<U>> getFormats() {
        return Collections.unmodifiableList(formats);
    }

    public void addFormat(AssetFormat<U> format) {
        formats.add(format);
        if (moduleEnvironment != null) {
            scanForFormat(format);
        }
    }

    private void scanForFormat(AssetFormat<U> format) {
        for (Module module : moduleEnvironment) {
            Map<Name, UnloadedAsset<U>> moduleSources = Maps.newLinkedHashMap();
            try {
                for (Path file : module.findFiles(FileScanning.acceptAll(), format.getFileMatcher(), ASSET_FOLDER, folderName)) {
                    try {
                        Name assetName = format.getAssetName(file.getFileName().toString());
                        UnloadedAsset<U> source = unloadedAssetLookup.get(assetName, module.getId());
                        if (source == null) {
                            source = new UnloadedAsset<>(new ResourceUrn(module.getId(), assetName), format);
                            moduleSources.put(assetName, source);
                        }
                        source.addInput(file);
                    } catch (InvalidAssetFilenameException e) {
                        logger.error("Invalid file name '{}' for asset type '{}", file.getFileName(), id, e);
                    }
                }
            } catch (IOException e) {
                logger.error("Failed to scan module '{}' for assets", module.getId(), e);
            }

            unloadedAssetLookup.column(module.getId()).putAll(moduleSources);
        }
    }

    public void removeFormat(AssetFormat<U> format) {
        formats.remove(format);
    }

    public void removeAllFormats() {
        formats.clear();
    }

    public T getAsset(ResourceUrn urn) {
        Preconditions.checkNotNull(urn);
        T asset = loadedAssets.get(urn);
        if (asset == null && moduleEnvironment != null && urn.getFragmentName().isEmpty()) {
            UnloadedAsset<U> source = unloadedAssetLookup.get(urn.getResourceName(), urn.getModuleName());
            if (source != null) {
                try {
                    U assetData = source.load();
                    return loadAsset(urn, assetData);
                } catch (IOException e) {
                    logger.error("Failed to load asset '" + urn + "'", e);
                }
            }
        }
        return asset;
    }

    public T getAsset(String urn) {
        if (ResourceUrn.isValid(urn)) {
            return getAsset(new ResourceUrn(urn));
        }
        return null;
    }

    public void dispose(ResourceUrn urn) {
        T asset = loadedAssets.remove(urn);
        if (asset != null) {
            asset.dispose();
        }
    }

    public T loadAsset(ResourceUrn urn, U data) {
        Preconditions.checkState(factory != null, "Factory not yet allocated for asset type '" + id + "'");

        T asset = loadedAssets.get(urn);
        if (asset != null) {
            asset.reload(data);
        } else {
            asset = factory.build(urn, data);
            loadedAssets.put(urn, asset);
        }

        return asset;
    }

    /**
     * @return The name of the sub-folder within modules that contains this asset type
     */
    public String getFolderName() {
        return folderName;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof AssetType) {
            AssetType other = (AssetType) obj;
            return id.equals(other.id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id.toString();
    }



}
