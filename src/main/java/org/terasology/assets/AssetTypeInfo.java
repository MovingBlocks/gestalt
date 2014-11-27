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

/**
 * AssetTypeInfo describes an asset type, with the information needed by the AssetManager to support this asset type.
 *
 * @author Immortius
 */
class AssetTypeInfo {

    private String folderName;
    private AssetType assetType;

    public AssetTypeInfo(AssetType assetType, String folderName) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(folderName), "folderName must not be null or empty");
        Preconditions.checkNotNull(assetType);
        this.folderName = folderName;
        this.assetType = assetType;
    }

    /**
     * @return The name of the sub-folder within modules that contains this asset type
     */
    public String getFolderName() {
        return folderName;
    }

    /**
     * @return The type identifier for this asset type
     */
    public AssetType getAssetType() {
        return assetType;
    }
}
