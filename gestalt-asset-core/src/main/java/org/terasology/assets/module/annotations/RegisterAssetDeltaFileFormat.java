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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@link org.terasology.assets.format.AssetAlterationFileFormat AssetAlterationFileFormat} to be automatically registered by
 * {@link org.terasology.assets.module.ModuleAwareAssetTypeManager ModuleAwareAssetTypeManager} on environment change to handle asset deltas.
 * Asset deltas provide modifications to assets originally declared in another module that the module providing the delta has a dependency on -
 * this allows for multiple modules providing modifications to an asset without necessarily wiping the changes from other modules.
 * <p>
 * This is an alternative to using overrides, which completely replace an asset provided by another module.
 * @author Immortius
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RegisterAssetDeltaFileFormat {
}
