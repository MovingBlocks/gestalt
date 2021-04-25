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

import org.terasology.context.annotation.API;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
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

    private final ResourceUrn urn;
    private final AssetType<?, T> assetType;
    private final DisposalHook disposalHook = new DisposalHook();
    private volatile boolean disposed;

    protected AssetNode<Asset<T>> next;
    protected Asset<?> parent;

    protected static class AssetNode<U extends Asset<?>> {
        public AssetNode(U root,U instance) {
            this.reference = new SoftReference<>(instance);
            instance.parent = root;
        }
        SoftReference<U> reference;
        AssetNode<U> next;

        protected void clearParent() {
            U instance = reference == null ? null: reference.get();
            if(instance != null) {
                instance.parent = null;
            }
        }

        protected void setParent(U parent) {
            U instance = reference == null ? null: reference.get();
            if(instance != null) {
                instance.parent = parent;
            }
        }

        protected boolean hasValidAsset() {
            U instance = reference == null ? null: reference.get();
            if(instance == null) {
                return false;
            }
            return !instance.isDisposed();
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
        return (Optional<U>) assetType.createInstance(this);
    }

    final synchronized Optional<? extends Asset<T>> createCopy(ResourceUrn copyUrn) {
        Preconditions.checkState(!disposed);
        return doCreateCopy(copyUrn, assetType);
    }

    /**
     * Disposes this asset, freeing resources and making it unusable
     */
    public final synchronized void dispose() {
        if (!disposed) {
            disposed = true;
            assetType.onAssetDisposed(this);
            disposalHook.dispose();
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
