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

/**
 * This package provides integration between the asset framework and gestalt-modules.
 * <p>
 * ModuleAwareAssetTypeManager automatically registers asset types, formats and producers from a module environment,
 * manages unloading and reloading assets over environment changes.
 * </p>
 * <p>
 * ModuleAssetDataProducer producing assets based on files within a module - this is set up automatically by ModuleAwareAssetTypeManager.
 * </p>
 */
package org.terasology.assets.module;

