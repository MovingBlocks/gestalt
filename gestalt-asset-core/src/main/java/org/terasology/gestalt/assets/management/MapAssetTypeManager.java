/*
 * Copyright 2019 MovingBlocks
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

package org.terasology.gestalt.assets.management;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Multimaps;

import net.jcip.annotations.ThreadSafe;

import org.reflections.ReflectionUtils;
import org.terasology.gestalt.assets.Asset;
import org.terasology.gestalt.assets.AssetData;
import org.terasology.gestalt.assets.AssetFactory;
import org.terasology.gestalt.assets.AssetType;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A simple implementation of AssetTypeManager based on {@link Map}.
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
        return Optional.ofNullable((AssetType<T, U>) assetTypes.get(type));
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

    @Override
    public Collection<AssetType<?, ?>> getAssetTypes() {
        return Collections.unmodifiableCollection(assetTypes.values());
    }

    @Override
    public void disposedUnusedAssets() {
        assetTypes.values().forEach(type -> type.processDisposal());
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
        addAssetType(assetType);

        return assetType;
    }

    /**
     * Adds an assetType. There must not be an existing asset type for the asset class managed by the asset type.
     *
     * @param assetType
     */
    public synchronized void addAssetType(AssetType<?, ?> assetType) {
        Preconditions.checkNotNull(assetType);
        Preconditions.checkState(assetTypes.get(assetType.getAssetClass()) == null, "Asset type already registered for - " + assetType.getAssetClass().getSimpleName());

        assetTypes.put(assetType.getAssetClass(), assetType);
        for (Class<?> parentType : ReflectionUtils.getAllSuperTypes(assetType.getAssetClass(), (Predicate<Class<?>>) input -> Asset.class.isAssignableFrom(input) && input != Asset.class)) {
            subtypes.put((Class<? extends Asset>) parentType, assetType.getAssetClass());
            (subtypes.get((Class<? extends Asset>) parentType)).sort(Comparator.comparing(Class::getSimpleName));

        }
    }

    /**
     * Removes an AssetType from the manager, closing it.
     *
     * @param type The type of Asset to remove the AssetType for
     * @param <T>  The type of Asset
     */
    @SuppressWarnings("unchecked")
    public synchronized <T extends Asset<?>> AssetType<?, ?> removeAssetType(Class<T> type) {
        AssetType<?, ?> assetType = assetTypes.remove(type);
        if (assetType != null) {
            assetType.close();
            for (Class<?> parentType : ReflectionUtils.getAllSuperTypes(type, (Predicate<Class<?>>) input -> Asset.class.isAssignableFrom(input) && input != Asset.class)) {
                subtypes.remove(parentType, type);
            }
        }
        return assetType;
    }

    /**
     * Removes all AssetTypes from the manager, closing all of them.
     */
    public synchronized void clear() {
        for (AssetType<?, ?> assetType : assetTypes.values()) {
            assetType.close();
        }
        assetTypes.clear();
        subtypes.clear();
    }


}
