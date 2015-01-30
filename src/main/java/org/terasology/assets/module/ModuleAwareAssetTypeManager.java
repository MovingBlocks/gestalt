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

package org.terasology.assets.module;

import com.google.common.collect.Maps;
import org.terasology.assets.Asset;
import org.terasology.assets.AssetData;
import org.terasology.assets.AssetType;
import org.terasology.assets.AssetTypeManager;

import java.util.Map;

/**
 * @author Immortius
 */
public class ModuleAwareAssetTypeManager implements AssetTypeManager {

    private Map<Class<? extends Asset>, AssetType<?, ?>> assetTypes = Maps.newHashMap();

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Asset<U>, U extends AssetData> AssetType<T, U> getAssetType(Class<T> type) {
        return (AssetType<T, U>) assetTypes.get(type);
    }



}
