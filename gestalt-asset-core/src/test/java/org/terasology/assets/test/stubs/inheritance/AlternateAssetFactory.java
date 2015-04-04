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

import org.terasology.assets.AssetFactory;
import org.terasology.assets.AssetType;
import org.terasology.assets.ResourceUrn;

/**
 * @author Immortius
 */
public class AlternateAssetFactory implements AssetFactory<AlternateAsset, AlternateAssetData> {
    @Override
    public AlternateAsset build(ResourceUrn urn, AssetType<AlternateAsset, AlternateAssetData> type, AlternateAssetData data) {
        return new AlternateAsset(urn, data, type);
    }
}
