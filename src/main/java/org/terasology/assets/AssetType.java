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
import com.google.common.collect.Maps;
import org.terasology.module.ModuleEnvironment;
import org.terasology.naming.Name;
import org.terasology.naming.ResourceUrn;

import java.util.Map;

public class AssetType<T extends Asset<U>, U extends AssetData> {

    private final Name id;
    private final String folderName;
    private final Class<T> assetClass;

    private ModuleEnvironment moduleEnvironment;
    private AssetFactory<T, U> factory;

    private Map<ResourceUrn, T> loadedAssets = Maps.newHashMap();

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

    public void setEnvironment(ModuleEnvironment environment) {
        this.moduleEnvironment = environment;
    }

    public void setFactory(AssetFactory<T, U> factory) {
        this.factory = factory;
        for (T asset : loadedAssets.values()) {
            asset.dispose();
        }
        loadedAssets.clear();
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

    public T getAsset(ResourceUrn urn) {
        return loadedAssets.get(urn);
    }
}
