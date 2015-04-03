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

package org.terasology.assets;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import org.terasology.assets.management.AssetManager;
import org.terasology.module.sandbox.API;
import org.terasology.naming.Name;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/**
 * An abstract implementation of AssetDataProducer aimed to ease the creation of producers that provide fragments from other assets.
 * <p>
 * A fragment is a piece of a parent asset that can be referenced individually. An example would be a single tile from an atlas texture - if the atlas is "engine:atlas",
 * a single tile might be identified as "engine:atlas#tile".
 * </p>
 * <p>
 * A Fragment AssetDataProducer thus resolves a parent asset, and then produces an AssetData from that. This abstract class handles the resolution of the of the parent
 * asset.
 * </p>
 *
 * @author Immortius
 */
@API
public abstract class AbstractFragmentDataProducer<T extends AssetData, U extends Asset<V>, V extends AssetData> implements AssetDataProducer<T> {

    private final AssetManager assetManager;
    private final Class<U> rootAssetType;
    private final boolean resolveModuleFromRoot;

    /**
     * It is expected that implementing classes will provide the class for the rootAssetType in their constructor.
     *
     * @param assetManager          The asset manager to use when resolving the root asset.
     * @param rootAssetType         The type of the root asset the fragments are produced from.
     * @param resolveModuleFromRoot Whether to resolve providing modules from the rootAssetType.
     *                              Should be disabled if the rootAssetType is the same as the fragment's asset type - otherwise resolution will infinitely loop.
     */
    protected AbstractFragmentDataProducer(AssetManager assetManager, Class<U> rootAssetType, boolean resolveModuleFromRoot) {
        this.assetManager = assetManager;
        this.rootAssetType = rootAssetType;
        this.resolveModuleFromRoot = resolveModuleFromRoot;
    }

    @Override
    public Set<Name> getModulesProviding(Name resourceName) {
        if (resolveModuleFromRoot) {
            return ImmutableSet.copyOf(Collections2.transform(assetManager.resolve(resourceName.toString(), rootAssetType), new Function<ResourceUrn, Name>() {
                @Nullable
                @Override
                public Name apply(ResourceUrn input) {
                    return input.getModuleName();
                }
            }));
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public ResourceUrn redirect(ResourceUrn urn) {
        return urn;
    }

    @Override
    public Optional<T> getAssetData(ResourceUrn urn) throws IOException {
        Optional<? extends U> rootAsset = assetManager.getAsset(urn.getRootUrn(), rootAssetType);
        if (rootAsset.isPresent()) {
            return getFragmentData(urn, rootAsset.get());
        }
        return Optional.empty();
    }

    /**
     * Implementing classes will implement this to produce fragments.
     *
     * @param urn       The urn of the requested fragment asset.
     * @param rootAsset The root asset to build the fragment from.
     * @return An optional that will contain the fragment's asset data, if available.
     */
    protected abstract Optional<T> getFragmentData(ResourceUrn urn, U rootAsset);
}
