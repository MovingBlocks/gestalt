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
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.module.sandbox.API;
import org.terasology.naming.Name;
import org.terasology.util.reflection.GenericsUtil;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * AssetType manages all assets of a particular type/class.  It provides the ability to resolve and load assets by Urn, and caches assets so that there is only
 * a single instance of a given asset shared by all users.
 * <p>
 * AssetType is thread safe.
 * </p>
 *
 * @param <T> The type of asset this AssetType manages
 * @param <U> The type of asset data required by the assets this AssetType manages
 */
@API
@ThreadSafe
public final class AssetType<T extends Asset<U>, U extends AssetData> implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(AssetType.class);

    private final Class<T> assetClass;
    private final Class<U> assetDataClass;
    private final AssetFactory<? extends T, U> factory;
    private final List<AssetDataProducer<U>> producers = Lists.newCopyOnWriteArrayList();
    private final Map<ResourceUrn, T> loadedAssets = new MapMaker().concurrencyLevel(4).makeMap();
    private final ListMultimap<ResourceUrn, T> instanceAssets = Multimaps.synchronizedListMultimap(ArrayListMultimap.<ResourceUrn, T>create());
    private boolean closed;
    private volatile ResolutionStrategy resolutionStrategy = (modules, context) -> {
        if (modules.contains(context)) {
            return ImmutableSet.of(context);
        } else {
            return modules;
        }
    };

    /**
     * Constructs an AssetType for managing assets of the provided Asset class. The Asset class must have its AssetData generic parameter bound via inheritance
     * (e.g. MyType extends Asset&lt;MyDataType&gt;)
     *
     * @param assetClass The class of asset this AssetType will manage.
     * @param factory    The factory used to convert AssetData to Assets for this type
     */
    @SuppressWarnings("unchecked")
    public AssetType(Class<T> assetClass, AssetFactory<? extends T, U> factory) {
        Preconditions.checkNotNull(assetClass);
        Preconditions.checkNotNull(factory);

        this.factory = factory;
        this.assetClass = assetClass;
        Optional<Type> assetDataType = GenericsUtil.getTypeParameterBindingForInheritedClass(assetClass, Asset.class, 0);
        if (assetDataType.isPresent()) {
            assetDataClass = (Class<U>) GenericsUtil.getClassOfType(assetDataType.get());
        } else {
            throw new IllegalArgumentException("Asset class must have a bound AssetData parameter - " + assetClass);
        }
    }

    /**
     * Closes the AssetType, disposing all assets, closing the producers and preventing further asset creation.
     */
    @Override
    public synchronized void close() {
        if (!closed) {
            closed = true;
            disposeAll();
            clearProducers();
        }
    }

    /**
     * @return Whether the AssetType is closed.
     */
    public synchronized boolean isClosed() {
        return closed;
    }

    /**
     * Disposes all assets of this type.
     */
    public synchronized void disposeAll() {
        for (T asset : loadedAssets.values()) {
            asset.dispose();
        }
        for (T asset : ImmutableList.copyOf(instanceAssets.values())) {
            asset.dispose();
        }
        if (!loadedAssets.isEmpty()) {
            logger.error("Assets remained loaded after disposal - " + loadedAssets.keySet());
            loadedAssets.clear();
        }
        if (!instanceAssets.isEmpty()) {
            logger.error("Asset instances remained loaded after disposal - " + instanceAssets.keySet());
            instanceAssets.clear();
        }
    }

    /**
     * Refreshes the AssetType. All loaded assets that are provided by the producers are reloaded, all other assets are disposed. Asset instances are reloaded with
     * the data of their parents or disposed along with them.
     * <p>
     * This method is useful when switching contexts (such as changing module environment)
     * </p>
     */
    public synchronized void refresh() {
        if (!closed) {
            for (T asset : loadedAssets.values()) {
                if (!followRedirects(asset.getUrn()).equals(asset.getUrn()) || !reloadFromProducers(asset)) {
                    asset.dispose();
                    for (T instance : ImmutableList.copyOf(instanceAssets.get(asset.getUrn().getInstanceUrn()))) {
                        instance.dispose();
                    }
                }
            }
        }
    }

    /**
     * @return The class of Asset managed by this AssetType.
     */
    public Class<T> getAssetClass() {
        return assetClass;
    }

    /**
     * @return The class of AssetData used to generate the Assets managed by this AssetType.
     */
    public Class<U> getAssetDataClass() {
        return assetDataClass;
    }

    /**
     * By default a simple strategy is used returns the context module if it is one of the options, and all the options otherwise.
     *
     * @param strategy The strategy used to filter modules during partial urn resolution.
     */
    public void setResolutionStrategy(ResolutionStrategy strategy) {
        this.resolutionStrategy = strategy;
    }

    /**
     * Adds an AssetDataProducer for generating assets of for this AssetType
     *
     * @param producer The producer to add
     */
    public synchronized void addProducer(AssetDataProducer<U> producer) {
        if (!closed) {
            producers.add(producer);
        }
    }

    /**
     * @return An unmodifiable list of all the AssetDataProducers
     */
    public List<AssetDataProducer<U>> getProducers() {
        return Collections.unmodifiableList(producers);
    }

    /**
     * @param producer The producer to remove;
     * @return Whether the producer was removed
     */
    public synchronized boolean removeProducer(AssetDataProducer<U> producer) {
        return producers.remove(producer);
    }

    /**
     * Removes all the AssetDataProducers
     */
    public synchronized void clearProducers() {
        producers.clear();
    }

    /**
     * Obtains an asset by urn, loading it if necessary. If the urn is a instance urn, then a new asset will be created from the parent asset.
     *
     * @param urn The urn of the resource to get
     * @return The asset if available
     */
    public Optional<T> getAsset(ResourceUrn urn) {
        Preconditions.checkNotNull(urn);
        if (urn.isInstance()) {
            return getInstanceAsset(urn);
        } else {
            return getNormalAsset(urn);
        }
    }

    /**
     * Notifies the asset type when an asset is disposed
     *
     * @param asset The asset that was disposed.
     */
    void onAssetDisposed(Asset<U> asset) {
        if (asset.getUrn().isInstance()) {
            instanceAssets.get(asset.getUrn()).remove(assetClass.cast(asset));
        } else {
            loadedAssets.remove(asset.getUrn());
        }
    }

    /**
     * Notifies the asset type when an asset is created
     *
     * @param asset The asset that was created
     */
    synchronized void registerAsset(Asset<U> asset) {
        if (asset.getUrn().isInstance()) {
            instanceAssets.put(asset.getUrn(), assetClass.cast(asset));
        } else {
            loadedAssets.put(asset.getUrn(), assetClass.cast(asset));
        }
    }

    /**
     * Creates and returns an instance of an asset, if possible. The following methods are used to create the copy, in order, with the first technique to succeeed used:
     * <ol>
     * <li>Delegate to the parent asset to create the copy</li>
     * <li>Loads the AssetData of the parent asset and create a new instance from that</li>
     * </ol>
     *
     * @param urn The urn of the asset to create an instance of
     * @return An instance of the desired asset
     */
    @SuppressWarnings("unchecked")
    public Optional<T> getInstanceAsset(ResourceUrn urn) {
        Optional<? extends T> parentAsset = getAsset(urn.getParentUrn());
        if (parentAsset.isPresent()) {
            return createInstance(parentAsset.get());
        } else {
            return Optional.empty();
        }
    }

    /**
     * Creates an instance of the given asset
     *
     * @param asset The asset to create an instance of
     * @return The new instance, or {@link Optional#empty} if it could not be created
     */
    Optional<T> createInstance(Asset<U> asset) {
        Preconditions.checkArgument(assetClass.isAssignableFrom(asset.getClass()));
        Optional<? extends Asset<U>> result = asset.createCopy(asset.getUrn().getInstanceUrn());
        if (!result.isPresent()) {
            try {
                for (AssetDataProducer<U> producer : producers) {
                    Optional<U> data = producer.getAssetData(asset.getUrn());
                    if (data.isPresent()) {
                        return Optional.of(loadAsset(asset.getUrn().getInstanceUrn(), data.get()));
                    }
                }
            } catch (IOException e) {
                logger.error("Failed to load asset '" + asset.getUrn().getInstanceUrn() + "'", e);
            }
        }
        return Optional.ofNullable(assetClass.cast(result.get()));
    }

    /**
     * Obtains a non-instance asset
     *
     * @param urn The urn of the asset
     * @return The asset if available
     */
    private Optional<T> getNormalAsset(ResourceUrn urn) {
        ResourceUrn redirectUrn = followRedirects(urn);
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
        return Optional.ofNullable(asset);
    }

    /**
     * Follows any redirects to determine the actual resource urn to use for a given urn
     *
     * @param urn The urn to resolve redirects for
     * @return The final urn to use
     */
    private ResourceUrn followRedirects(ResourceUrn urn) {
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

    /**
     * Obtains an asset from a string that may be a full or partial urn
     *
     * @param urn The full or partial urn of the asset
     * @return The requested asset if the urn was successfully resolved
     */
    public Optional<T> getAsset(String urn) {
        return getAsset(urn, Name.EMPTY);
    }

    /**
     * Obtains an asset from a string that may be a full or partial urn
     *
     * @param urn           The full or partial urn of the asset
     * @param moduleContext The context to resolve the urn in
     * @return The requested asset if the urn was successfully resolved
     */
    public Optional<T> getAsset(String urn, Name moduleContext) {
        Set<ResourceUrn> resolvedUrns = resolve(urn, moduleContext);
        if (resolvedUrns.size() == 1) {
            return getAsset(resolvedUrns.iterator().next());
        } else if (resolvedUrns.size() > 1) {
            logger.warn("Failed to resolve asset '{}' - multiple possibilities discovered", urn);
        } else {
            logger.warn("Failed to resolve asset '{}' - no matches found", urn);
        }
        return Optional.empty();
    }

    /**
     * Resolves a string urn that may be a full or partial urn, providing the available urns that match
     *
     * @param urn The string to resolve
     * @return A set of possible matching urns
     */
    public Set<ResourceUrn> resolve(String urn) {
        return resolve(urn, Name.EMPTY);
    }

    /**
     * Resolves a string urn that may be a full or partial urn, providing the available urns that match
     *
     * @param urn           The string to resolve
     * @param moduleContext The context to resolve within
     * @return A set of possible matching urns
     */
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

    /**
     * Reloads an asset from the current producers, if one of them can produce it
     *
     * @param asset The asset to reload
     * @return Whether the asset was reloaded
     */
    private boolean reloadFromProducers(Asset<U> asset) {
        try {
            for (AssetDataProducer<U> producer : producers) {
                Optional<U> data = producer.getAssetData(asset.getUrn());

                if (data.isPresent()) {
                    asset.reload(data.get());
                    for (T assetInstance : instanceAssets.get(asset.getUrn().getInstanceUrn())) {
                        assetInstance.reload(data.get());
                    }
                    return true;
                }
            }
        } catch (IOException e) {
            logger.error("Failed to reload asset '{}', disposing");
        }
        return false;
    }

    /**
     * Loads an asset with the given urn and data. If the asset already exists, it is reloaded with the data instead
     *
     * @param urn  The urn of the asset
     * @param data The data to load the asset with
     * @return The loaded (or reloaded) asset
     */
    public T loadAsset(ResourceUrn urn, U data) {
        if (urn.isInstance()) {
            return factory.build(urn, this, data);
        } else {
            T asset = loadedAssets.get(urn);
            if (asset != null) {
                asset.reload(data);
            } else {
                synchronized (this) {
                    if (!closed) {
                        asset = loadedAssets.get(urn);
                        if (asset == null) {
                            asset = factory.build(urn, this, data);
                            loadedAssets.put(urn, asset);
                        } else {
                            asset.reload(data);
                        }
                    }
                }
            }

            return asset;
        }
    }

    /**
     * @param urn The urn of the asset to check. Must not be an instance urn
     * @return Whether an asset is loaded with the given urn
     */
    public boolean isLoaded(ResourceUrn urn) {
        Preconditions.checkArgument(!urn.isInstance(), "Urn must not be an instance urn");
        return loadedAssets.containsKey(urn);
    }

    /**
     * @return A set of the urns of all the loaded assets.
     */
    public Set<ResourceUrn> getLoadedAssetUrns() {
        return ImmutableSet.copyOf(loadedAssets.keySet());
    }

    /**
     * @return A list of all the loaded assets.
     */
    public Set<T> getLoadedAssets() {
        return ImmutableSet.copyOf(loadedAssets.values());
    }

    /**
     * @return A set of the urns of all the loaded assets and all the assets available from producers
     */
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
