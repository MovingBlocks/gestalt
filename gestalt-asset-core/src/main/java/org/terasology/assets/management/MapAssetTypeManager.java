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
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Multimaps;
import org.reflections.ReflectionUtils;
import org.terasology.assets.Asset;
import org.terasology.assets.AssetData;
import org.terasology.assets.AssetFactory;
import org.terasology.assets.AssetType;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * A basically Map-based AssetTypeManager.
 *
 * @author Immortius
 */
@ThreadSafe
public final class MapAssetTypeManager implements AssetTypeManager {

    private final Map<Class<? extends Asset>, AssetType<?, ?>> assetTypes = new MapMaker().concurrencyLevel(1).makeMap();
    private final ListMultimap<Class<? extends Asset>, Class<? extends Asset>> subtypes =
            Multimaps.synchronizedListMultimap(ArrayListMultimap.<Class<? extends Asset>, Class<? extends Asset>>create());

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Asset<U>, U extends AssetData> Optional<AssetType<T, U>> getAssetType(Class<T> type) {
        return Optional.fromNullable((AssetType<T, U>) assetTypes.get(type));

    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Asset<?>> List<AssetType<? extends T, ?>> getAssetTypes(Class<T> type) {
        List<AssetType<? extends T, ?>> result = Lists.newArrayList();
        for (Class<? extends Asset> subtype : ImmutableList.copyOf(subtypes.get(type))) {
            result.add((AssetType<? extends T, ?>) assetTypes.get(subtype));
        }
        return result;
    }

    /**
     * Creates a new asset type for the given class of asset. There must not be an existing asset type for that class.
     *
     * @param type    The class of Asset to create a type for
     * @param factory The factory that will create the assets
     * @param <T>     The type of Asset
     * @param <U>     The type of AssetData required by the Asset.
     * @return The new AssetType
     */
    @SuppressWarnings("unchecked")
    public synchronized <T extends Asset<U>, U extends AssetData> AssetType<T, U> createAssetType(Class<T> type, AssetFactory<T, U> factory) {
        Preconditions.checkState(assetTypes.get(type) == null, "Asset type already created - " + type.getSimpleName());
        Preconditions.checkNotNull(factory);
        Preconditions.checkNotNull(type);

        AssetType<T, U> assetType = new AssetType<>(type, factory);
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

    /**
     * Removes an AssetType from the manager, closing it.
     *
     * @param type The type of Asset to remove the AssetType for
     * @param <T>  The type of Asset
     */
    @SuppressWarnings("unchecked")
    public synchronized <T extends Asset<?>> void removeAssetType(Class<T> type) {
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
