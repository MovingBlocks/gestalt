// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.gestalt.assets;

import android.support.annotation.Nullable;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Sets;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.context.annotation.API;
import org.terasology.gestalt.naming.Name;
import org.terasology.gestalt.util.reflection.GenericsUtil;

import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

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
    private final Map<ResourceUrn, Reference<T>> loadedAssets = new MapMaker().concurrencyLevel(4).makeMap();

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
        Set<ResourceUrn> urns = new HashSet<>();
        Reference<? extends Asset<U>> ref = null;
        while ((ref = disposalQueue.poll()) != null) {
            AssetReference<? extends Asset<U>> assetRef = (AssetReference<? extends Asset<U>>) ref;
            urns.add(assetRef.parentUrn);
            assetRef.dispose();
            references.remove(assetRef);
        }
        for (ResourceUrn urn : urns) {
            disposeAsset(urn);
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
        loadedAssets.values().forEach(k -> {
            Asset<U> asset = k.get();
            if (asset != null) {
                asset.dispose();
            }
        });
        loadedAssets.clear();
        processDisposal();
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
            for (Reference<T> target : loadedAssets.values()) {
                T asset = target.get();
                if (asset != null && (!followRedirects(asset.getUrn()).equals(asset.getUrn()) || !reloadFromProducers(asset))) {
                    asset.dispose();
                    for(WeakReference<Asset<U>> it :asset.instances()) {
                        Asset<U> instance = it.get();
                        if(instance != null) {
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
     * Notifies the asset type when an asset is created
     *
     * @param asset The asset that was created
     */
    synchronized void registerAsset(Asset<U> asset, DisposalHook disposer) {
        if (closed) {
            throw new IllegalStateException("Cannot create asset for disposed asset type: " + assetClass);
        } else {
            if (!asset.getUrn().isInstance()) {
                loadedAssets.put(asset.getUrn(), new SoftReference<T>(assetClass.cast(asset)));
            }
            references.add(new AssetReference<>(asset, disposalQueue, disposer));
        }
    }

    /**
     * dispose asset and remove loaded asset from {@link #loadedAssets}
     * @param target urn to free
     */
    private void disposeAsset(ResourceUrn target) {
        Preconditions.checkArgument(!target.isInstance());
        if (!loadedAssets.containsKey(target)) {
            return;
        }

        Reference<T> reference = loadedAssets.get(target);
        Asset<U> current = reference.get();
        if (current == null) {
            loadedAssets.remove(target);
        } else if (current.isDisposed()) {
            for (WeakReference<Asset<U>> it : current.instances()) {
                Asset<U> instance = it.get();
                if (instance != null && !instance.isDisposed()) {
                    logger.warn("non instanced asset is disposed with instances. instances will become orphaned.");
                    break;
                }
            }
            loadedAssets.remove(target);
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
     *
     */
    @SuppressWarnings("unchecked")
    public Optional<T> getInstanceAsset(ResourceUrn urn) {
        Optional<? extends T> parentAsset = getAsset(urn.getParentUrn());
        if (parentAsset.isPresent() && !parentAsset.get().isDisposed()) {
            return parentAsset.get().createInstance();
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
        return asset.createInstance();
    }


    public Optional<U> fetchAssetData(ResourceUrn urn) throws PrivilegedActionException {
        return AccessController.doPrivileged((PrivilegedExceptionAction<Optional<U>>) () -> {
            for (AssetDataProducer<U> producer : producers) {
                Optional<U> data = producer.getAssetData(urn);
                if (data.isPresent()) {
                    return data;
                }
            }
            return Optional.empty();
        });
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
            Optional<U> data = fetchAssetData(urn);
            if (data.isPresent()) {
                return Optional.of(loadAsset(redirectUrn, data.get()));
            }
            Reference<T>  reference = loadedAssets.get(redirectUrn);
            return Optional.ofNullable(reference == null ? null : reference.get());
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
            Reference<T> reference = loadedAssets.get(urn);
            T asset = reference == null ? null : reference.get();
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
                        reference = loadedAssets.get(urn);
                        asset = reference == null ? null : reference.get();
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
     * Obtains a non-instance asset
     *
     * @param urn The urn of the asset
     * @return The asset if available
     */
    private Optional<T> getNormalAsset(ResourceUrn urn) {
        ResourceUrn redirectUrn = followRedirects(urn);
        Reference<T> reference = loadedAssets.get(redirectUrn);
        T asset = reference == null ? null : reference.get();
        if (asset == null) {
            return reload(redirectUrn);
        }
        if (asset.isDisposed()) {
            return Optional.empty();
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
                    for(WeakReference<Asset<U>> it :asset.instances()) {
                        Asset<U> instance = it.get();
                        if(instance != null) {
                            instance.reload(data.get());
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
        return loadedAssets.values().stream().map(Reference::get).filter(Objects::nonNull).collect(Collectors.toSet());
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

    private static final class AssetReference<T extends Asset<?>> extends PhantomReference<T> {

        private final DisposalHook disposalHook;
        public final ResourceUrn parentUrn;
        public AssetReference(T asset, ReferenceQueue<T> queue, DisposalHook hook) {
            super(asset, queue);
            this.disposalHook = hook;
            parentUrn = asset.getUrn().getParentUrn();
        }

        public void dispose() {
            disposalHook.dispose();
        }
    }
}
