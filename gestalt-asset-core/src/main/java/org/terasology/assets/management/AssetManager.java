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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import net.jcip.annotations.ThreadSafe;

import org.terasology.assets.Asset;
import org.terasology.assets.AssetData;
import org.terasology.assets.AssetType;
import org.terasology.assets.ResourceUrn;
import org.terasology.module.sandbox.API;
import org.terasology.naming.Name;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * AssetManager provides an simplified interface for working with assets across multiple asset types.
 * <p>
 * To do this it uses an {@link AssetTypeManager} to obtain the AssetTypes relating to an Asset
 * class of interest, and delegates down to them for actions such as obtaining and reloading assets.
 * </p>
 *
 * @author Immortius
 */
@API
@ThreadSafe
public final class AssetManager {

    private final AssetTypeManager assetTypeManager;

    /**
     * @param assetTypeManager the asset type manager that will be used
     */
    public AssetManager(AssetTypeManager assetTypeManager) {
        this.assetTypeManager = assetTypeManager;
    }

    /**
     * @param urn  The urn of the asset to check. Must not be an instance urn
     * @param type The Asset class of interest
     * @return whether an asset is loaded with the given urn
     */
    public boolean isLoaded(ResourceUrn urn, Class<? extends Asset<?>> type) {
        for (AssetType<?, ?> assetType : assetTypeManager.getAssetTypes(type)) {
            if (assetType.isLoaded(urn)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieves a set of the ResourceUrns for all loaded assets of the given Asset class (including subtypes)
     *
     * @param type The Asset class of interest
     * @param <T>  The Asset class
     * @return A set of the ResourceUrns of all loaded assets
     */
    public <T extends Asset<?>> Set<ResourceUrn> getLoadedAssetUrns(Class<T> type) {
        List<AssetType<? extends T, ?>> assetTypes = assetTypeManager.getAssetTypes(type);
        switch (assetTypes.size()) {
            case 0:
                return Collections.emptySet();
            case 1:
                return assetTypes.get(0).getLoadedAssetUrns();
            default:
                Set<ResourceUrn> result = Sets.newLinkedHashSet();
                for (AssetType<? extends T, ?> assetType : assetTypes) {
                    result.addAll(assetType.getLoadedAssetUrns());
                }
                return result;
        }
    }

    /**
     * Retrieves a list of all loaded assets of the given Asset class (including subtypes)
     *
     * @param type The Asset class of interest
     * @param <T>  The Asset class
     * @param <U>  The AssetData class
     * @return A list of all the loaded assets
     */
    public <T extends Asset<U>, U extends AssetData> Set<T> getLoadedAssets(Class<T> type) {
        List<AssetType<? extends T, ?>> assetTypes = assetTypeManager.getAssetTypes(type);
        switch (assetTypes.size()) {
            case 0:
                return Collections.emptySet();
            default:
                ImmutableSet.Builder<T> builder = ImmutableSet.builder();
                for (AssetType<? extends T, ?> assetType : assetTypes) {
                    builder.addAll(assetType.getLoadedAssets());
                }
                return builder.build();
        }
    }

    /**
     * Retrieves a set of the ResourceUrns for all available assets of the given Asset class (including subtypes). An available asset is either a loaded asset, or one
     * which can be requested. The set is not necessarily complete as assets procedurally generated from their resource urn may not be included.
     *
     * @param type The Asset class of interest
     * @param <T>  The Asset class
     * @return A set of the ResourceUrns of all available assets
     */
    public <T extends Asset<?>> Set<ResourceUrn> getAvailableAssets(Class<T> type) {
        List<AssetType<? extends T, ?>> assetTypes = assetTypeManager.getAssetTypes(type);
        switch (assetTypes.size()) {
            case 0:
                return Collections.emptySet();
            case 1:
                return assetTypes.get(0).getAvailableAssetUrns();
            default:
                Set<ResourceUrn> result = Sets.newLinkedHashSet();
                for (AssetType<? extends T, ?> assetType : assetTypes) {
                    result.addAll(assetType.getAvailableAssetUrns());
                }
                return result;
        }
    }

    /**
     * Given a string that may be a partial or full urn, resolves all possible ResourceUrns to load it as. This takes into account the current module context from
     * {@link org.terasology.assets.management.ContextManager}.
     *
     * @param urn  The full or partial urn to resolve.
     * @param type The type of Asset to resolve this the urn for
     * @param <T>  The class of Asset
     * @return A set of possible ResourceUrns that match these conditions
     */
    public <T extends Asset<?>> Set<ResourceUrn> resolve(String urn, Class<T> type) {
        return resolve(urn, type, ContextManager.getCurrentContext());
    }

    /**
     * Given a string that may be a partial or full urn, resolves all possible ResourceUrns to load it as. This uses the module context given.
     *
     * @param urn           The full or partial urn to resolve.
     * @param type          The type of Asset to resolve this the urn for
     * @param moduleContext The module context to resolve the urn within
     * @param <T>           The class of Asset
     * @return A set of possible ResourceUrns that match these conditions
     */
    public <T extends Asset<?>> Set<ResourceUrn> resolve(String urn, Class<T> type, Name moduleContext) {
        List<AssetType<? extends T, ?>> assetTypes = assetTypeManager.getAssetTypes(type);
        switch (assetTypes.size()) {
            case 0:
                return Collections.emptySet();
            case 1:
                return assetTypes.get(0).resolve(urn, moduleContext);
            default:
                Set<ResourceUrn> result = Sets.newLinkedHashSet();
                for (AssetType<? extends T, ?> assetType : assetTypes) {
                    result.addAll(assetType.resolve(urn, moduleContext));
                }
                return result;
        }
    }

    /**
     * Retrieves an asset from a full or partial urn, of the given Asset type. The urn is resolved as per {@link #resolve(String, Class)}
     *
     * @param urn  The full or partial urn of the asset to retrieve
     * @param type The type of Asset to retrieve
     * @param <T>  The class of Asset
     * @param <U>  The class of AssetData
     * @return An optional containing the requested asset if successfully obtained.
     */
    public <T extends Asset<U>, U extends AssetData> Optional<T> getAsset(String urn, Class<T> type) {
        return getAsset(urn, type, ContextManager.getCurrentContext());
    }

    /**
     * Retrieves an asset from a full or partial urn, of the given Asset type
     *
     * @param urn           The full or partial urn of the asset to retrieve
     * @param type          The type of Asset to retrieve
     * @param moduleContext The context in which to resolve the urn
     * @param <T>           The class of Asset
     * @param <U>           The class of AssetData
     * @return An Optional containing the requested asset if successfully obtained
     */
    public <T extends Asset<U>, U extends AssetData> Optional<T> getAsset(String urn, Class<T> type, Name moduleContext) {
        Set<ResourceUrn> resourceUrns = resolve(urn, type, moduleContext);
        if (resourceUrns.size() == 1) {
            return getAsset(resourceUrns.iterator().next(), type);
        }
        return Optional.empty();
    }

    /**
     * Retrieves an asset with the given urn and type
     *
     * @param urn  The urn of the asset to retrieve
     * @param type The type of asset to retrieve
     * @param <T>  The class of Asset
     * @param <U>  The class of AssetData
     * @return An Optional containing the requested asset if successfully obtained
     */
    public <T extends Asset<U>, U extends AssetData> Optional<T> getAsset(ResourceUrn urn, Class<T> type) {
        List<AssetType<? extends T, ?>> assetTypes = assetTypeManager.getAssetTypes(type);
        switch (assetTypes.size()) {
            case 0:
                return Optional.empty();
            case 1: {
                Optional<? extends T> result = assetTypes.get(0).getAsset(urn);
                if (result.isPresent()) {
                    return Optional.of(result.get());
                }
                break;
            }
            default:
                for (AssetType<? extends T, ?> assetType : assetTypes) {
                    Optional<? extends T> result = assetType.getAsset(urn);
                    if (result.isPresent()) {
                        return Optional.of(result.get());
                    }
                }
                break;
        }
        return Optional.empty();
    }

    /**
     * Creates or reloads an asset with the given urn, data and type. The type must be the actual type of the asset, not a super type.
     *
     * @param urn  The urn of the asset
     * @param data The data to load the asset with
     * @param type The type of the asset
     * @param <T>  The class of Asset
     * @param <U>  The class of AssetData
     * @return The loaded asset
     * @throws java.lang.IllegalStateException if the asset type is not managed by this AssetManager.
     */
    public <T extends Asset<U>, U extends AssetData> T loadAsset(ResourceUrn urn, U data, Class<T> type) {
        Optional<AssetType<T, U>> assetType = assetTypeManager.getAssetType(type);
        if (assetType.isPresent()) {
            return assetType.get().loadAsset(urn, data);
        } else {
            throw new IllegalStateException(type + " is not a supported type of asset");
        }
    }

}
