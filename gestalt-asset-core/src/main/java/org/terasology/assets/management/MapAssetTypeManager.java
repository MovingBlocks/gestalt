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

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.reflections.ReflectionUtils;
import org.terasology.assets.Asset;
import org.terasology.assets.AssetData;
import org.terasology.assets.AssetType;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * @author Immortius
 */
public final class MapAssetTypeManager implements AssetTypeManager {

    private Map<Class<? extends Asset>, AssetType<?, ?>> assetTypes = Maps.newHashMap();
    private ListMultimap<Class<? extends Asset>, Class<? extends Asset>> subtypes = ArrayListMultimap.create();

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Asset<U>, U extends AssetData> AssetType<T, U> getAssetType(Class<T> type) {
        return (AssetType<T, U>) assetTypes.get(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Asset<U>, U extends AssetData> List<AssetType<? extends T, ? extends U>> getAssetTypes(Class<T> type) {
        List<AssetType<? extends T, ? extends U>> result = Lists.newArrayList();
        for (Class<? extends Asset> subtype : subtypes.get(type)) {
            result.add((AssetType<? extends T, ? extends U>) assetTypes.get(subtype));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public <T extends Asset<U>, U extends AssetData> AssetType<T, U> createAssetType(Class<T> type) {
        Preconditions.checkState(assetTypes.get(type) == null, "Asset type already created - " + type.getSimpleName());

        AssetType<T, U> assetType = new AssetType<>(type);
        assetTypes.put(type, assetType);
        for (Class<?> parentType : ReflectionUtils.getAllSuperTypes(type, new Predicate<Class<?>>() {
            @Override
            public boolean apply(Class<?> input) {
                return Asset.class.isAssignableFrom(input) && input != Asset.class;
            }
        })) {
            subtypes.put((Class<? extends Asset>) parentType, type);
            Collections.sort(subtypes.get((Class<? extends Asset>) parentType), new Comparator<Class<?>>() {
                @Override
                public int compare(Class<?> o1, Class<?> o2) {
                    return o1.getSimpleName().compareTo(o2.getSimpleName());
                }
            });
        }

        return assetType;
    }

    @SuppressWarnings("unchecked")
    public <T extends Asset<U>, U extends AssetData> void removeAssetType(Class<T> type) {
        AssetType<?, ?> assetType = assetTypes.remove(type);
        if (assetType != null) {
            assetType.close();
            for (Class<?> parentType : ReflectionUtils.getAllSuperTypes(type, new Predicate<Class<?>>() {
                @Override
                public boolean apply(Class<?> input) {
                    return Asset.class.isAssignableFrom(input) && input != Asset.class;
                }
            })) {
                subtypes.remove(parentType, type);
            }
        }
    }


}
