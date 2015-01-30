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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.naming.Name;
import org.terasology.naming.ResourceUrn;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AssetType<T extends Asset<U>, U extends AssetData> {

    private static final Logger logger = LoggerFactory.getLogger(AssetType.class);

    private final Class<T> assetClass;
    private List<AssetProducer<U>> producers = Lists.newArrayList();
    private AssetFactory<T, U> factory;
    private Map<ResourceUrn, T> loadedAssets = Maps.newHashMap();

    public AssetType(Class<T> assetClass) {
        Preconditions.checkNotNull(assetClass);

        this.assetClass = assetClass;
    }

    public Class<T> getAssetClass() {
        return assetClass;
    }

    public AssetFactory<T, U> getFactory() {
        return factory;
    }

    public void setFactory(AssetFactory<T, U> factory) {
        this.factory = factory;
        for (T asset : loadedAssets.values()) {
            asset.dispose();
        }
        loadedAssets.clear();
    }

    public void addProducer(AssetProducer<U> producer) {
        producers.add(producer);
    }

    public List<AssetProducer<U>> getProducers() {
        return Collections.unmodifiableList(producers);
    }

    public void removeProducer(AssetProducer<U> producer) {
        producers.remove(producer);
    }

    public void clearProducers() {
        producers.clear();
    }

    public T getAsset(ResourceUrn urn) {
        Preconditions.checkNotNull(urn);

        ResourceUrn redirectUrn = redirect(urn);
        T asset = loadedAssets.get(redirectUrn);
        if (asset == null) {
            try {
                for (AssetProducer<U> producer : producers) {
                    U data = producer.getAssetData(redirectUrn);
                    if (data != null) {
                        asset = loadAsset(redirectUrn, data);
                    }
                }
            } catch (IOException e) {
                if (redirectUrn.equals(urn)) {
                    logger.error("Failed to load asset '" + redirectUrn + "'", e);
                } else {
                    logger.error("Failed to load asset '" + redirectUrn + "' redirected from '" + urn + "'", e);
                }
            }
        }
        return asset;
    }

    private ResourceUrn redirect(ResourceUrn urn) {
        ResourceUrn result = urn;
        for (AssetProducer<U> producer : producers) {
            result = producer.redirect(urn);
        }
        return result;
    }

    public T getAsset(String urn) {
        return getAsset(urn, Name.EMPTY);
    }

    public T getAsset(String urn, Name moduleContext) {
        Set<ResourceUrn> resolvedUrns = resolve(urn, moduleContext);
        if (resolvedUrns.size() == 1) {
            return getAsset(resolvedUrns.iterator().next());
        } else if (resolvedUrns.size() > 1) {
            logger.warn("Failed to resolve asset '{}' - multiple possibilities discovered", urn);
        } else {
            logger.warn("Failed to resolve asset '{}' - no matches found", urn);
        }
        return null;
    }

    public Set<ResourceUrn> resolve(String urn) {
        return resolve(urn, Name.EMPTY);
    }

    public Set<ResourceUrn> resolve(String urn, Name moduleContext) {
        if (ResourceUrn.isValid(urn)) {
            return ImmutableSet.of(new ResourceUrn(urn));
        }

        Set<ResourceUrn> results = Sets.newLinkedHashSet();
        for (AssetProducer<U> producer : producers) {
            results.addAll(producer.resolve(urn, moduleContext));
        }
        return results;
    }

    public void dispose(ResourceUrn urn) {
        T asset = loadedAssets.remove(urn);
        if (asset != null) {
            asset.dispose();
        }
    }

    public T loadAsset(ResourceUrn urn, U data) {
        Preconditions.checkState(factory != null, "Factory not yet allocated for asset type '" + assetClass.getSimpleName() + "'");

        T asset = loadedAssets.get(urn);
        if (asset != null) {
            asset.reload(data);
        } else {
            asset = factory.build(urn, data);
            loadedAssets.put(urn, asset);
        }

        return asset;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof AssetType) {
            AssetType other = (AssetType) obj;
            return assetClass.equals(other.assetClass);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return assetClass.hashCode();
    }

    @Override
    public String toString() {
        return assetClass.getSimpleName();
    }

}
