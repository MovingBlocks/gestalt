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

/**
 * This package provides an Asset System - a way of creating, loading and managing assets. An asset is a shared resource that can be resolved by a uri or partial uri. For a
 * game, some potential assets are sounds, textures, and object definitions.
 * <p/>
 * The main classes of the asset system are:
 * <ul>
 *     <li>AssetData, an implementation independent representation of an asset</li>
 *     <li>AssetFormat, </li>
 * </ul>
 * The asset system features support for:
 * <ul>
 *      <li>Different types of assets</li>
 *      <li>One or more file formats for assets</li>
 *      <li>Supplemental file formats for assets - these can be used to add additional metadata to an asset in another file format</li>
 *      <li></li>
 *      <li></li>
 * </ul>
 */
// TODO: More javadoc
@API package org.terasology.assets;

import org.terasology.module.sandbox.API;
