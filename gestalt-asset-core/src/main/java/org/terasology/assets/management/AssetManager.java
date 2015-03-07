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

package org.terasology.assets.management;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import org.terasology.assets.Asset;
import org.terasology.assets.AssetData;
import org.terasology.assets.AssetType;
import org.terasology.assets.ResourceUrn;
import org.terasology.assets.management.AssetTypeManager;
import org.terasology.assets.management.ContextManager;
import org.terasology.naming.Name;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Immortius
 */
public final class AssetManager {

    private AssetTypeManager assetTypeManager;

    public AssetManager(AssetTypeManager assetTypeManager) {
        this.assetTypeManager = assetTypeManager;
    }

    public <T extends Asset<U>, U extends AssetData> Set<ResourceUrn> getLoadedAssets(Class<T> type) {
        List<AssetType<? extends T, ? extends U>> assetTypes = assetTypeManager.getAssetTypes(type);
        switch (assetTypes.size()) {
            case 0:
                return Collections.emptySet();
            case 1:
                return assetTypes.get(0).getLoadedAssetUrns();
            default:
                Set<ResourceUrn> result = Sets.newLinkedHashSet();
                for (AssetType<? extends T, ? extends U> assetType : assetTypes) {
                    result.addAll(assetType.getLoadedAssetUrns());
                }
                return result;
        }
    }

    public <T extends Asset<U>, U extends AssetData> Set<ResourceUrn> getAvailableAssets(Class<T> type) {
        List<AssetType<? extends T, ? extends U>> assetTypes = assetTypeManager.getAssetTypes(type);
        switch (assetTypes.size()) {
            case 0:
                return Collections.emptySet();
            case 1:
                return assetTypes.get(0).getAvailableAssetUrns();
            default:
                Set<ResourceUrn> result = Sets.newLinkedHashSet();
                for (AssetType<? extends T, ? extends U> assetType : assetTypes) {
                    result.addAll(assetType.getAvailableAssetUrns());
                }
                return result;
        }
    }

    public <T extends Asset<U>, U extends AssetData> Set<ResourceUrn> resolve(String urn, Class<T> type) {
        return resolve(urn, type, ContextManager.getCurrentContext());
    }

    public <T extends Asset<U>, U extends AssetData> Set<ResourceUrn> resolve(String urn, Class<T> type, Name moduleContext) {
        List<AssetType<? extends T, ? extends U>> assetTypes = assetTypeManager.getAssetTypes(type);
        switch (assetTypes.size()) {
            case 0:
                return Collections.emptySet();
            case 1:
                return assetTypes.get(0).resolve(urn, moduleContext);
            default:
                Set<ResourceUrn> result = Sets.newLinkedHashSet();
                for (AssetType<? extends T, ? extends U> assetType : assetTypes) {
                    result.addAll(assetType.resolve(urn, moduleContext));
                }
                return result;
        }
    }

    public <T extends Asset<U>, U extends AssetData> Optional<? extends T> getAsset(String urn, Class<T> type) {
        return getAsset(urn, type, ContextManager.getCurrentContext());
    }

    public <T extends Asset<U>, U extends AssetData> Optional<? extends T> getAsset(String urn, Class<T> type, Name moduleContext) {
        Set<ResourceUrn> resourceUrns = resolve(urn, type, moduleContext);
        if (resourceUrns.size() == 1) {
            return getAsset(resourceUrns.iterator().next(), type);
        }
        return Optional.absent();
    }

    public <T extends Asset<U>, U extends AssetData> Optional<? extends T> getAsset(ResourceUrn urn, Class<T> type) {
        List<AssetType<? extends T, ? extends U>> assetTypes = assetTypeManager.getAssetTypes(type);
        switch (assetTypes.size()) {
            case 0:
                return Optional.absent();
            case 1:
                return assetTypes.get(0).getAsset(urn);
            default:
                for (AssetType<? extends T, ? extends U> assetType : assetTypes) {
                    Optional<? extends T> result = assetType.getAsset(urn);
                    if (result.isPresent()) {
                        return result;
                    }
                }
        }
        return Optional.absent();
    }

    public <T extends Asset<U>, U extends AssetData> T loadAsset(ResourceUrn urn, U data, Class<T> type) {
        AssetType<T, U> assetType = assetTypeManager.getAssetType(type);
        return assetType.loadAsset(urn, data);
    }

    public <T extends Asset<U>, U extends AssetData> T createInstance(T asset, Class<T> type) {
        AssetType<T, U> assetType = assetTypeManager.getAssetType(type);
        return assetType.createInstance(asset);
    }
}
