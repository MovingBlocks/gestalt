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

package org.terasology.assets.module.annotations;

import org.terasology.assets.AssetFactory;
import org.terasology.module.sandbox.API;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an {@link org.terasology.assets.Asset Asset} to be automatically registered as an asset type by
 * {@link org.terasology.assets.module.ModuleAwareAssetTypeManager ModuleAwareAssetTypeManager} on environment change.
 *
 * <p>Note that asset types loaded in this way will be unloaded when switching environments, and all assets disposed. If an asset type should persist across environment
 * changes and assets reloaded instead they should be manually registered as a core asset type. This will be typically only be the case for assets types from the
 * classpath module(s)</p>
 * <p>
 * By default the AssetFactory must either have an empty constructor, or one taking an AssetManager
 * </p>
 * @author Immortius
 */
@API
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RegisterAssetType {
    /**
     * @return The subdirectory where assets of this type will be discovered. Can be omitted for asset types that are not loaded from files.
     */
    String[] folderName() default {};

    /**
     * @return The factory class to use when generating assets of this type
     */
    Class<? extends AssetFactory> factoryClass();
}
