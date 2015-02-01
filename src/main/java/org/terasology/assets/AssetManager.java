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

import org.terasology.naming.Name;
import org.terasology.naming.ResourceUrn;

/**
 * @author Immortius
 */
public final class AssetManager {

    private AssetTypeManager assetTypeManager;

    public AssetManager(AssetTypeManager assetTypeManager) {
        this.assetTypeManager = assetTypeManager;
    }

    public <T extends Asset<U>, U extends AssetData> T getAsset(String urn, Class<T> type) {
        AssetType<T, U> assetType = assetTypeManager.getAssetType(type);
        return assetType.getAsset(urn, ContextManager.getCurrentContext());
    }

    public <T extends Asset<U>, U extends AssetData> T getAsset(String urn, Class<T> type, Name moduleContext) {
        AssetType<T, U> assetType = assetTypeManager.getAssetType(type);
        return assetType.getAsset(urn, moduleContext);
    }

    public <T extends Asset<U>, U extends AssetData> T getAsset(ResourceUrn urn, Class<T> type) {
        AssetType<T, U> assetType = assetTypeManager.getAssetType(type);
        return assetType.getAsset(urn);
    }

    public <T extends Asset<U>, U extends AssetData> T loadAsset(ResourceUrn urn, U data, Class<T> type) {
        AssetType<T, U> assetType = assetTypeManager.getAssetType(type);
        return assetType.loadAsset(urn, data);
    }

    public <T extends Asset<U>, U extends AssetData> void dispose(T asset) {
        AssetType<? extends Asset, AssetData> assetType = assetTypeManager.getAssetType(asset.getClass());
        assetType.dispose(asset.getUrn());
    }
}
