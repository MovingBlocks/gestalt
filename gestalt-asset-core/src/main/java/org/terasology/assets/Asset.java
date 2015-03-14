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

import com.google.common.base.Preconditions;
import org.terasology.module.sandbox.API;

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
public abstract class Asset<T extends AssetData> {

    private final ResourceUrn urn;
    private final AssetType<?, T> assetType;
    private boolean disposed;

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
     */
    public final synchronized void reload(T data) {
        Preconditions.checkState(!disposed);
        doReload(data);
    }

    /**
     * Creates an instance of this asset. The instance will have the same urn as this asset, with the instance flag appended, and initially have the same data and settings.
     * <p>
     * Instance assets are reloaded back to the same value as their origin if their asset type is refreshed.
     * </p>
     *
     * @param <U> The type of the asset to return (this is a bit of a hack since there is no way to force the return value to have the type of the object instance)
     * @return A new instance of the asset.
     */
    @SuppressWarnings("unchecked")
    public final synchronized <U extends Asset<T>> U createInstance() {
        U instance = (U) doCreateInstance(urn.getInstanceUrn(), assetType);
        assetType.registerInstance(instance);
        return instance;
    }

    /**
     * Disposes this asset, freeing resources and making it unusable
     */
    public final synchronized void dispose() {
        if (!disposed) {
            disposed = true;
            doDispose();
            assetType.containedAssetDisposed(this);
        }
    }

    /**
     * Called to reload an asset with the given data.
     *
     * @param data The data to load.
     */
    protected abstract void doReload(T data);

    /**
     * Called if an instance of this asset is required. An instance is an independent copy of an asset, but identified in terms of its origin - so if the reference
     * to the asset is saved and reloaded a new copy of the parent can be provided.
     * <p>
     * Implementing classes should essentially create a copy of the asset. This may be done by creating an AssetData of the current asset and using it to create
     * a new asset, or may need to use more implementation specific methods (an OpenGL texture may use an OpenGL texture handle copy technique to produce the
     * copy, for example)
     * </p>
     *
     * @param instanceUrn The urn for the new instance
     * @return The created instance.
     */
    protected abstract Asset<T> doCreateInstance(ResourceUrn instanceUrn, AssetType<?, T> parentAssetType);

    /**
     * Called to dispose an asset. If the asset uses any resources that need to be manually cleaned up, this is where it should be done. This will only ever
     * be called once per asset.
     */
    protected abstract void doDispose();

    /**
     * @return Whether this asset has been disposed
     */
    public final synchronized boolean isDisposed() {
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
