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

package org.terasology.assets;

import android.support.annotation.Nullable;

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

import net.jcip.annotations.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.module.sandbox.API;
import org.terasology.naming.Name;
import org.terasology.util.reflection.GenericsUtil;

import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Semaphore;

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
    private final AssetFactory<T, U> factory;
    private final List<AssetDataProducer<U>> producers = Lists.newCopyOnWriteArrayList();
    private final Map<ResourceUrn, T> loadedAssets = new MapMaker().concurrencyLevel(4).makeMap();
    private final ListMultimap<ResourceUrn, WeakReference<T>> instanceAssets = Multimaps.synchronizedListMultimap(ArrayListMultimap.<ResourceUrn, WeakReference<T>>create());

    // Per-asset locks to deal with situations where multiple threads attempt to obtain or create the same unloaded asset concurrently
    private final Map<ResourceUrn, ResourceLock> locks = new MapMaker().concurrencyLevel(1).makeMap();

    private final Set<AssetReference<? extends Asset<U>>> references = Sets.newConcurrentHashSet();
    private final ReferenceQueue<Asset<U>> disposalQueue = new ReferenceQueue<>();

    private volatile boolean closed;

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
    public AssetType(Class<T> assetClass, AssetFactory<T, U> factory) {
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
     * Disposes any assets queued for disposal. This occurs if an asset is no longer referenced by anything.
     */
    @SuppressWarnings("unchecked")
    public void processDisposal() {
        Reference<? extends Asset<U>> ref = disposalQueue.poll();
        while (ref != null) {
            AssetReference<? extends Asset<U>> assetRef = (AssetReference<? extends Asset<U>>) ref;
            assetRef.dispose();
            references.remove(assetRef);
            ref = disposalQueue.poll();
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
        loadedAssets.values().forEach(T::dispose);

        for (WeakReference<T> assetRef : ImmutableList.copyOf(instanceAssets.values())) {
            T asset = assetRef.get();
            if (asset != null) {
                asset.dispose();
            }
        }
        processDisposal();
        if (!loadedAssets.isEmpty()) {
            logger.error("Assets remained loaded after disposal - {}", loadedAssets.keySet());
            loadedAssets.clear();
        }
        if (!instanceAssets.isEmpty()) {
            logger.error("Asset instances remained loaded after disposal - {}", instanceAssets.keySet());
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
    public void refresh() {
        if (!closed) {
            for (T asset : loadedAssets.values()) {
                if (!followRedirects(asset.getUrn()).equals(asset.getUrn()) || !reloadFromProducers(asset)) {
                    asset.dispose();
                    for (WeakReference<T> instanceRef : ImmutableList.copyOf(instanceAssets.get(asset.getUrn().getInstanceUrn()))) {
                        T instance = instanceRef.get();
                        if (instance != null) {
                            instance.dispose();
                        }
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
            instanceAssets.get(asset.getUrn()).remove(new WeakReference<>(assetClass.cast(asset)));
        } else {
            loadedAssets.remove(asset.getUrn());
        }
    }

    /**
     * Notifies the asset type when an asset is created
     *
     * @param asset The asset that was created
     */
    synchronized void registerAsset(Asset<U> asset, DisposalHook disposer) {
        if (closed) {
            throw new RuntimeException("Cannot create asset for disposed asset type: " + assetClass);
        } else {
            if (asset.getUrn().isInstance()) {
                instanceAssets.put(asset.getUrn(), new WeakReference<>(assetClass.cast(asset)));
            } else {
                loadedAssets.put(asset.getUrn(), assetClass.cast(asset));
            }
            references.add(new AssetReference<>(asset, disposalQueue, disposer));
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
                return AccessController.doPrivileged((PrivilegedExceptionAction<Optional<T>>) () -> {
                    for (AssetDataProducer<U> producer : producers) {
                        Optional<U> data = producer.getAssetData(asset.getUrn());
                        if (data.isPresent()) {
                            return Optional.of(loadAsset(asset.getUrn().getInstanceUrn(), data.get()));
                        }
                    }
                    return Optional.ofNullable(assetClass.cast(result.get()));
                });
            } catch (PrivilegedActionException e) {
                logger.error("Failed to load asset '" + asset.getUrn().getInstanceUrn() + "'", e.getCause());
            }
        }
        return Optional.ofNullable(assetClass.cast(result.get()));
    }

    /**
     * Forces a reload of an asset from a data producer, if possible.  The resource urn must not be an instance urn (it doesn't make sense to reload an instance by urn).
     * If there is no available source for the asset (it has no producer) then it will not be reloaded.
     *
     * @param urn The urn of the resource to reload.
     * @return The asset if it exists (regardless of whether it was reloaded or not)
     */
    public Optional<T> reload(ResourceUrn urn) {
        Preconditions.checkArgument(!urn.isInstance(), "Cannot reload an asset instance urn");
        ResourceUrn redirectUrn = followRedirects(urn);
        try {
            return AccessController.doPrivileged((PrivilegedExceptionAction<Optional<T>>) () -> {
                for (AssetDataProducer<U> producer : producers) {
                    Optional<U> data = producer.getAssetData(redirectUrn);
                    if (data.isPresent()) {
                        return Optional.of(loadAsset(redirectUrn, data.get()));
                    }
                }
                return Optional.ofNullable(loadedAssets.get(redirectUrn));
            });
        } catch (PrivilegedActionException e) {
            if (redirectUrn.equals(urn)) {
                logger.error("Failed to load asset '{}'", redirectUrn, e.getCause());
            } else {
                logger.error("Failed to load asset '{}' redirected from '{}'", redirectUrn, urn, e.getCause());
            }
        }
        return Optional.empty();
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
            return reload(redirectUrn);
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
                    for (WeakReference<T> assetInstanceRef : instanceAssets.get(asset.getUrn().getInstanceUrn())) {
                        T assetInstance = assetInstanceRef.get();
                        if (assetInstance != null) {
                            assetInstance.reload(data.get());
                        }
                    }
                    return true;
                }
            }
        } catch (IOException e) {
            logger.error("Failed to reload asset '{}', disposing", asset.getUrn());
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
                ResourceLock lock;
                synchronized (locks) {
                    lock = locks.computeIfAbsent(urn, k -> new ResourceLock(urn));
                }
                try {
                    lock.lock();
                    if (!closed) {
                        asset = loadedAssets.get(urn);
                        if (asset == null) {
                            asset = factory.build(urn, this, data);
                        } else {
                            asset.reload(data);
                        }
                    }
                    synchronized (locks) {
                        if (lock.unlock()) {
                            locks.remove(urn);
                        }
                    }
                } catch (InterruptedException e) {
                    logger.error("Failed to load asset - interrupted awaiting lock on resource {}", urn);
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


    private static final class ResourceLock {
        private final ResourceUrn urn;
        private final Semaphore semaphore = new Semaphore(1);

        public ResourceLock(ResourceUrn urn) {
            this.urn = urn;
        }

        public void lock() throws InterruptedException {
            semaphore.acquire();
        }

        public boolean unlock() {
            boolean lockFinished = !semaphore.hasQueuedThreads();
            semaphore.release();
            return lockFinished;
        }

        @Override
        public String toString() {
            return "lock(" + urn + ")";
        }
    }

    private static final class AssetReference<T> extends PhantomReference<T> {

        private final DisposalHook disposalHook;

        public AssetReference(T asset, ReferenceQueue<T> queue, DisposalHook hook) {
            super(asset, queue);
            this.disposalHook = hook;
        }

        public void dispose() {
            disposalHook.dispose();
        }
    }
}
