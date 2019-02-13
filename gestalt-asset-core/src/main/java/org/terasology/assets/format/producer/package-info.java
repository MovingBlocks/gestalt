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
 * This package contains support for producing assets from files, leveraging the file format interfaces from org.terasology.assets.format
 * <p>
 * {@link org.terasology.assets.format.producer.AssetFileDataProducer} is the core class - a producer that will load assets from files. The formats for handling the assets,
 * asset supplements and asset deltas can be registered with the producer, and then the producer can be notified of which files exist (or are added/changed/removed over time).
 * When an asset is later requested it will use the registered formats to load the asset from file.
 */
package org.terasology.assets.format.producer;
