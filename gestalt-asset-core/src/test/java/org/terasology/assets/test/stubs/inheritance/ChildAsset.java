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

package org.terasology.assets.test.stubs.inheritance;

import org.terasology.assets.Asset;
import org.terasology.assets.AssetType;
import org.terasology.assets.ResourceUrn;

import java.util.Optional;

/**
 * @author Immortius
 */
public class ChildAsset extends ParentAsset<ChildAssetData> {

    public ChildAsset(ResourceUrn urn, ChildAssetData data, AssetType<?, ChildAssetData> type) {
        super(urn, type);
        doReload(data);
    }

    @Override
    protected Optional<? extends Asset<ChildAssetData>> doCreateCopy(ResourceUrn instanceUrn, AssetType<?, ChildAssetData> parentAssetType) {
        return Optional.of(new ChildAsset(instanceUrn, new ChildAssetData(), parentAssetType));
    }

    @Override
    protected void doReload(ChildAssetData data) {

    }

}
