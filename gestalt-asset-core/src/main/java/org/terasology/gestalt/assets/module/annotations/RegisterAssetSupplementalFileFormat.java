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

package org.terasology.gestalt.assets.module.annotations;

import org.terasology.context.annotation.API;
import org.terasology.context.annotation.Index;
import org.terasology.gestalt.assets.module.ModuleAwareAssetTypeManagerImpl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@link org.terasology.gestalt.assets.format.AssetAlterationFileFormat AssetAlterationFileFormat} to be automatically registered by
 * {@link ModuleAwareAssetTypeManagerImpl ModuleAwareAssetTypeManager} on environment change to handle asset supplemental data files.
 * Supplemental data files are common across multiple file formats. For example, textures may support both '.png' and '.bmp' files (with one AssetFormat for each),
 * but also support secondary '.info' file that provides additional information such as the runtime compression or clamping technique to use. Supplemental file formats
 * support this use case.
 * <p>
 * Supplemental file formats are used both for loading standard asset and overrides.
 * </p>
 * <p>
 * By default the AssetAlterationFormat must have an empty constructor, or one taking an AssetManager
 * </p>
 *
 * @author Immortius
 */
@API
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Index
public @interface RegisterAssetSupplementalFileFormat {
}
