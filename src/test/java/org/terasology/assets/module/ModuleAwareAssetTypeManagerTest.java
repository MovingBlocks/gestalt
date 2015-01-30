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

package org.terasology.assets.module;

/**
 * @author Immortius
 */
public class ModuleAwareAssetTypeManagerTest {

    // Two types of assets types:
    // * Core (from the class path)
    // * Extension (loaded from modules)
    //
    // Core will be manually loaded, and won't be removed on environment change.
    // Extension will be automatically loaded and removed on environment change.
    //
    // Asset
    // AssetData
    // AssetFactory
    // AssetProducers
    // AssetFormat
    // Asset Delta Format
    // Asset Supplemental Format
    //
    // Assets will be cleaned up upon environment change, with all assets either disposed or reloaded.
    //
    // Texture (asset)
    // LwjglTexture (assetImpl) <- drive registration through this?
    // TextureData (assetData)
    // ModuleAssetProducer
    //  - PngFormat <- Automatically pick up these
    //  - TextureMetadataFormat  <- Automatically pick up these
    //  - (delta format) <- Automatically pick up these
    // ColorTextureProducer  <- Automatically pick up these
    //
    //
    // Plan of attack:
    // - Manage core asset types
    // - Automatic disposal/reload of assets on environment change
    // - Manage extension formats
    // - Manage extension asset types

}
