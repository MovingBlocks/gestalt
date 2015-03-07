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
 * This package contains the interfaces for loading asset data from files.
 * <p>
 * A file format applies information from one or more files to create or alter an {@link org.terasology.assets.AssetData AssetData}.
 * {@link org.terasology.assets.format.AssetFileFormat AssetFileFormat} creates AssetData, while
 * {@link org.terasology.assets.format.AssetAlterationFileFormat AssetAlterationFileFormat} modifies an existing AssetData.
 */
@API
package org.terasology.assets.format;

import org.terasology.module.sandbox.API;
