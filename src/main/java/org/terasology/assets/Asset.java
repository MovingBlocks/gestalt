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

import org.terasology.naming.ResourceUrn;

/**
 * Interface common to all assets.
 * <p/>
 * An asset is a resource that is used by the game - a texture, sound, block definition and the like. These are typically
 * loaded from a module, although they can also be created at runtime. Each asset is identified by a ResourceUrn that uniquely
 * identifies it and can be used to obtain it. This urn provides a lightweight way to serialize a reference to an Asset.
 * <p/>
 * Assets are created from a specific type of asset data - this allows for implementation specific assets (e.g. OpenGL vs DirectX textures for example).
 * <p/>
 * Assets may be reloaded by providing a new batch of data, or disposed to free resources - disposed assets may no
 * longer be used until reloaded.
 *
 * @author Immortius
 */
public interface Asset<T extends AssetData> {

    /**
     * @return This asset's identifying URI.
     */
    ResourceUrn getUrn();

    /**
     * Reloads this assets using the new data.
     *
     * @param data
     */
    void reload(T data);

    /**
     * Disposes this asset, freeing resources and making it unusable
     */
    void dispose();

    /**
     * @return Whether this asset has been disposed
     */
    boolean isDisposed();
}
