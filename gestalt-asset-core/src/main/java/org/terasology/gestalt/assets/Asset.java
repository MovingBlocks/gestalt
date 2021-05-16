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

package org.terasology.gestalt.assets;

import com.google.common.base.Preconditions;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.context.annotation.API;

import java.lang.ref.WeakReference;
import java.security.PrivilegedActionException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;

/**
 * Abstract base class common to all assets.
 * <p>
 * An asset is a resource that is used by the game - a texture, sound, block definition and the like. These are typically
 * loaded from a module, although they can also be created at runtime. Each asset is identified by a ResourceUrn that uniquely
 * identifies it and can be used to obtain it. This urn provides a lightweight way to serialize a reference to an Asset.
 * </p>
 * <p>
 * Assets are created from a specific type of asset data. There may be a multiple implementations with a common base for a particular type of asset data
 * - this allows for implementation specific assets (e.g. OpenGL vs DirectX textures for example might have an OpenGLTexture and DirectXTexture implementing class
 * respectively, with a common Texture base class).
 * </p>
 * <p>
 * Assets may be reloaded by providing a new batch of data, or disposed to free resources - disposed assets may no
 * longer be used.
 * </p>
 * <p>
 * To support making Asset implementations thread safe reloading, creating an instance and disposal are all synchronized.
 * Implementations should consider thread safety around any methods they add if it is intended for assets to be used across multiple threads.
 * </p>
 *
 * @author Immortius
 */
@API
@ThreadSafe
public abstract class Asset<T extends AssetData> {
    private static final Logger logger = LoggerFactory.getLogger(Asset.class);

    private AssetNode<T> next;
    private Asset<T> parent;

    private final ResourceUrn urn;
    private final AssetType<?, T> assetType;
    private final DisposalHook disposalHook = new DisposalHook();
    private volatile boolean disposed;

    private static class AssetNode<T extends AssetData> {
        WeakReference<Asset<T>> reference;
        AssetNode<T> next;

        public AssetNode(Asset<T> root, Asset<T> instance) {
            this.reference = new WeakReference<>(instance);
            instance.parent = root;
        }

        protected boolean hasValidAsset() {
            Asset<T> instance = reference == null ? null: reference.get();
            if(instance == null) {
                return false;
            }
            return !instance.isDisposed();
        }
    }


    private void pushAsset(Asset<T> asset) {
        if (next == null) {
            Preconditions.checkArgument(!getUrn().isInstance());
            next = new AssetNode<>(this, asset);
        } else {
            AssetNode node = new AssetNode<>(parent, asset);
            node.next = next;
            next = node;
        }
    }

    protected Iterable<WeakReference<Asset<T>>> instances() {
        if (this.parent != null) {
            return parent.instances();
        }
        AssetNode<T> rootNode = this.next;
        if(rootNode == null) {
            return Collections::emptyIterator;
        }
        return () -> new Iterator<>() {
            AssetNode<T> node = null;

            @Override
            public boolean hasNext() {
                if (node == null) {
                    return true;
                }
                return node.next != null;
            }

            @Override
            public WeakReference<Asset<T>> next() {
                if (node == null) {
                    node = rootNode.next;
                    return rootNode.reference;
                }
                node = node.next;
                return node.reference;
            }
        };
    }

    protected void cleanup() {
        if(this.parent != null) {
            parent.cleanup();
            return;
        }
        AssetNode<T> node = next;
        while (node != null) {
            AssetNode<T> nextAsset = node.next;
            while (nextAsset != null && !nextAsset.hasValidAsset()) {
                nextAsset = nextAsset.next;
            }
            node.next = nextAsset;
            node = nextAsset;
        }
    }

    /**
     * The constructor for an asset. It is suggested that implementing classes provide a constructor taking both the urn, and an initial AssetData to load.
     *
     * @param urn       The urn identifying the asset.
     * @param assetType The asset type this asset belongs to.
     */
    protected Asset(ResourceUrn urn, AssetType<?, T> assetType) {
        Preconditions.checkNotNull(urn);
        Preconditions.checkNotNull(assetType);
        this.urn = urn;
        this.assetType = assetType;
        assetType.registerAsset(this, disposalHook);
    }

    /**
     * set a resource handler so the disposable hook can clean up resources not managed by the JVM
     * @param resource        A resource to close when disposing this class.  The resource must not have a reference to this asset -
     *                       this would prevent it being garbage collected. It must be a static inner class, or not contained in the asset class
     *                       (or an anonymous class defined in a static context). A warning will be logged if this is not the case.
     */
    protected void setDisposableResource(DisposableResource resource) {
        this.disposalHook.setDisposableResource(resource);
    }

    /**
     * @return This asset's identifying ResourceUrn.
     */
    public final ResourceUrn getUrn() {
        return urn;
    }

    /**
     * Reloads this assets using the new data.
     *
     * @param data The data to reload the asset with.
     * @throws org.terasology.gestalt.assets.exceptions.InvalidAssetDataException If the asset data is invalid or cannot be loaded
     */
    public final synchronized void reload(T data) {
        if (!disposed) {
            doReload(data);
        } else {
            throw new IllegalStateException("Cannot reload disposed asset '" + getUrn() + "'");
        }
    }

    /**
     * Creates an instance of this asset. The instance will have the same urn as this asset, with the instance flag appended, and initially have the same data and settings.
     * <p>
     * Instance assets are reloaded back to the same value as their origin if their asset type is refreshed.
     * </p>
     *
     * @param <U> The asset type
     * @return A new instance of the asset.
     */
    @SuppressWarnings("unchecked")
    public final <U extends Asset<T>> Optional<U> createInstance() {
        Preconditions.checkState(!disposed);
        ResourceUrn instanceUrn = getUrn().getInstanceUrn();

        Optional<? extends Asset<T>> assetCopy = doCreateCopy(instanceUrn, assetType);
        if (!assetCopy.isPresent()) {
            Optional<T> assetData;
            try {
                assetData = assetType.fetchAssetData(instanceUrn);
            } catch (PrivilegedActionException e) {
                logger.error("Failed to load asset '" + urn + "'", e.getCause());
                return Optional.empty();
            }
            if (assetData.isPresent()) {
                assetCopy = Optional.ofNullable(assetType.loadAsset(instanceUrn, assetData.get()));
            }
        }
        assetCopy.ifPresent(this::pushAsset);
        return (Optional<U>) assetCopy;
    }

    public final Asset<T> getNormalAsset() {
        if(parent == null) {
            return this;
        }
        return parent;
    }

    /**
     * Disposes this asset, freeing resources and making it unusable
     */
    public final synchronized void dispose() {
        if (!disposed) {
            disposed = true;
            assetType.onAssetDisposed(this);
            disposalHook.dispose();
            if(parent == null) {
                for (WeakReference<Asset<T>> inst : this.instances()) {
                    Asset<T> current = inst.get();
                    if (current != null) {
                        current.dispose();
                    }
                }
            }
        }
    }

    /**
     * Called to reload an asset with the given data.
     *
     * @param data The data to load.
     * @throws org.terasology.gestalt.assets.exceptions.InvalidAssetDataException If the asset data is invalid or cannot be loaded
     */
    protected abstract void doReload(T data);

    /**
     * Attempts to create a copy of the asset, with the given urn. This is used as part of the process of creating an asset instance.
     * <p>
     * If direct copies are not supported, then {@link Optional#empty} should be returned.
     * </p>
     * <p>
     * Implementing classes should create a copy of the asset. This may be done by creating an AssetData of the current asset and using it to create
     * a new asset, or may need to use more implementation specific methods (an OpenGL texture may use an OpenGL texture handle copy technique to produce the
     * copy, for example)
     * </p>
     *
     * @param copyUrn         The urn for the new instance
     * @param parentAssetType The type of the parent asset
     * @return The created copy if any
     */
    protected Optional<? extends Asset<T>> doCreateCopy(ResourceUrn copyUrn, AssetType<?, T> parentAssetType) {
        return Optional.empty();
    }

    /**
     * @return Whether this asset has been disposed
     */
    public final boolean isDisposed() {
        return disposed;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Asset) {
            Asset other = (Asset) obj;
            return !urn.isInstance() && !other.urn.isInstance() && other.urn.equals(urn);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return urn.hashCode();
    }

    @Override
    public String toString() {
        return urn.toString();
    }

}
