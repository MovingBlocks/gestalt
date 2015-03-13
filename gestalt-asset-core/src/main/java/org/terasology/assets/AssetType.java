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
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.naming.Name;
import org.terasology.util.reflection.GenericsUtil;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

// TODO
public final class AssetType<T extends Asset<U>, U extends AssetData> extends AssetOwner<U> implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(AssetType.class);

    private final Class<T> assetClass;
    private final Class<U> assetDataClass;
    private List<AssetDataProducer<U>> producers = Lists.newArrayList();
    private AssetFactory<T, U> factory;
    private Map<ResourceUrn, T> loadedAssets = Maps.newHashMap();
    private ResolutionStrategy resolutionStrategy = new ResolutionStrategy() {
        @Override
        public Set<Name> resolve(Set<Name> modules, Name context) {
            if (modules.contains(context)) {
                return ImmutableSet.of(context);
            } else {
                return modules;
            }
        }
    };

    @SuppressWarnings("unchecked")
    public AssetType(Class<T> assetClass) {
        Preconditions.checkNotNull(assetClass);

        this.assetClass = assetClass;
        Optional<Type> assetDataType = GenericsUtil.getTypeParameterBindingForInheritedClass(assetClass, Asset.class, 0);
        if (assetDataType.isPresent()) {
            assetDataClass = (Class<U>) GenericsUtil.getClassOfType(assetDataType.get());
        } else {
            throw new IllegalArgumentException("Asset class must have bound AssetData parameter - " + assetClass);
        }
    }

    @Override
    public void close() {
        disposeAll();
        for (AssetDataProducer<U> producer : producers) {
            producer.close();
        }
    }

    public Class<T> getAssetClass() {
        return assetClass;
    }

    public Class<U> getAssetDataClass() {
        return assetDataClass;
    }

    public AssetFactory<T, U> getFactory() {
        return factory;
    }

    public void setFactory(AssetFactory<T, U> factory) {
        this.factory = factory;
        disposeAll();
    }

    public void setResolutionStrategy(ResolutionStrategy strategy) {
        this.resolutionStrategy = strategy;
    }

    public void addProducer(AssetDataProducer<U> producer) {
        producers.add(producer);
    }

    public List<AssetDataProducer<U>> getProducers() {
        return Collections.unmodifiableList(producers);
    }

    public void removeProducer(AssetDataProducer<U> producer) {
        producers.remove(producer);
    }

    public void clearProducers() {
        producers.clear();
    }

    public Optional<T> getAsset(ResourceUrn urn) {
        Preconditions.checkNotNull(urn);
        if (urn.isInstance()) {
            return getInstanceAsset(urn);
        } else {
            return getNormalAsset(urn);
        }
    }

    public T createInstance(T existing) {
        Preconditions.checkNotNull(existing);
        T result = assetClass.cast(existing.doCreateInstance(existing.getUrn().getInstanceUrn()));
        if (result != null && !result.getOwner().isPresent()) {
            result.setOwner(existing);
            existing.addChild(result);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Optional<T> getInstanceAsset(ResourceUrn urn) {
        Optional<T> parentAsset = getAsset(urn.getParentUrn());
        if (parentAsset.isPresent()) {
            return Optional.of(createInstance(parentAsset.get()));
        } else {
            return Optional.absent();
        }
    }

    private Optional<T> getNormalAsset(ResourceUrn urn) {
        ResourceUrn redirectUrn = redirect(urn);
        T asset = loadedAssets.get(redirectUrn);
        if (asset == null) {
            try {
                for (AssetDataProducer<U> producer : producers) {
                    Optional<U> data = producer.getAssetData(redirectUrn);
                    if (data.isPresent()) {
                        asset = loadAsset(redirectUrn, data.get());
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
        return Optional.fromNullable(asset);
    }

    private ResourceUrn redirect(ResourceUrn urn) {
        ResourceUrn lastUrn;
        ResourceUrn finalUrn = urn;
        do {
            lastUrn = finalUrn;
            for (AssetDataProducer<U> producer : producers) {
                finalUrn = producer.redirect(finalUrn);
            }
        } while (!lastUrn.equals(finalUrn));
        return finalUrn;
    }

    public Optional<T> getAsset(String urn) {
        return getAsset(urn, Name.EMPTY);
    }

    public Optional<T> getAsset(String urn, Name moduleContext) {
        Set<ResourceUrn> resolvedUrns = resolve(urn, moduleContext);
        if (resolvedUrns.size() == 1) {
            return getAsset(resolvedUrns.iterator().next());
        } else if (resolvedUrns.size() > 1) {
            logger.warn("Failed to resolve asset '{}' - multiple possibilities discovered", urn);
        } else {
            logger.warn("Failed to resolve asset '{}' - no matches found", urn);
        }
        return Optional.absent();
    }

    public Set<ResourceUrn> resolve(String urn) {
        return resolve(urn, Name.EMPTY);
    }

    public Set<ResourceUrn> resolve(String urn, Name moduleContext) {
        if (ResourceUrn.isValid(urn)) {
            return ImmutableSet.of(new ResourceUrn(urn));
        }

        String urnToResolve = urn;
        final boolean instance = urn.endsWith(ResourceUrn.INSTANCE_INDICATOR);
        if (instance) {
            urnToResolve = urn.substring(0, urn.length() - ResourceUrn.INSTANCE_INDICATOR.length());
        }
        int fragmentSeparatorIndex = urnToResolve.indexOf('#');
        final Name fragmentName;
        final Name resourceName;
        if (fragmentSeparatorIndex != -1) {
            resourceName = new Name(urnToResolve.substring(0, fragmentSeparatorIndex));
            fragmentName = new Name(urnToResolve.substring(fragmentSeparatorIndex + 1));
        } else {
            resourceName = new Name(urnToResolve);
            fragmentName = Name.EMPTY;
        }

        Set<Name> possibleModules = Sets.newLinkedHashSet();
        for (AssetDataProducer<U> producer : producers) {
            possibleModules.addAll(producer.getModulesProviding(resourceName));
        }
        if (!moduleContext.isEmpty()) {
            possibleModules = resolutionStrategy.resolve(possibleModules, moduleContext);
        }
        return Sets.newLinkedHashSet(Collections2.transform(possibleModules, new Function<Name, ResourceUrn>() {
            @Nullable
            @Override
            public ResourceUrn apply(Name input) {
                return new ResourceUrn(input, resourceName, fragmentName, instance);
            }
        }));
    }

    @Override
    void onOwnedAssetDisposed(Asset<U> asset) {
        loadedAssets.remove(asset.getUrn());
    }

    public void refresh() {
        for (Map.Entry<ResourceUrn, T> entry : ImmutableMap.copyOf(loadedAssets).entrySet()) {
            if (!redirect(entry.getKey()).equals(entry.getKey()) || !reloadFromProducers(entry.getKey(), entry.getValue())) {
                entry.getValue().dispose();
            }
        }
    }

    private boolean reloadFromProducers(ResourceUrn urn, Asset<U> asset) {
        try {
            for (AssetDataProducer<U> producer : producers) {
                Optional<U> data = producer.getAssetData(urn);

                if (data.isPresent()) {
                    asset.reload(data.get());
                    return true;
                }
            }
        } catch (IOException e) {
            logger.error("Failed to reload asset '{}', disposing");
        }
        return false;
    }

    public void disposeAll() {
        for (T asset : ImmutableList.copyOf(loadedAssets.values())) {
            asset.dispose();
        }
        if (!loadedAssets.isEmpty()) {
            logger.error("Assets remained loaded after disposal - " + loadedAssets.keySet());
            loadedAssets.clear();
        }
    }

    public T loadAsset(ResourceUrn urn, U data) {
        Preconditions.checkState(factory != null, "Factory not yet allocated for asset type '" + assetClass.getSimpleName() + "'");

        T asset = loadedAssets.get(urn);
        if (asset != null) {
            asset.reload(data);
        } else {
            asset = factory.build(urn, data);
            asset.setOwner(this);
            loadedAssets.put(urn, asset);
        }

        return asset;
    }

    public boolean isLoaded(ResourceUrn urn) {
        return loadedAssets.containsKey(urn);
    }

    public Set<ResourceUrn> getLoadedAssetUrns() {
        return ImmutableSet.copyOf(loadedAssets.keySet());
    }

    public Set<ResourceUrn> getAvailableAssetUrns() {
        Set<ResourceUrn> availableAssets = Sets.newLinkedHashSet(getLoadedAssetUrns());
        for (AssetDataProducer<U> producer : producers) {
            availableAssets.addAll(producer.getAvailableAssetUrns());
        }
        return availableAssets;
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
