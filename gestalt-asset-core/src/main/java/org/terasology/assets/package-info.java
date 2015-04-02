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

/**
 * This package provides an Asset System - a way of creating, loading and managing assets. An asset is a shared resource that can be resolved by a uri or partial uri. For a
 * game, some potential assets are sounds, textures, and object definitions.
 * <p>
 * The main classes of the asset system are:
 * </p>
 * <ul>
 *     <li>AssetType, a manager for a type of Asset</li>
 *     <li>Asset, a fully loaded asset. This should be subclassed for each type of asset</li>
 *     <li>ResourceUrn, the identifier for an asset</li>
 *     <li>AssetData, an implementation independent representation of an asset. This should be subclassed for each type of asset</li>
 *     <li>AssetDataProducer, a provider of AssetData</li>
 *     <li>AssetFactory, that converts an AssetData into  full blown Asset</li>
 * </ul>
 */
package org.terasology.assets;

