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

package org.terasology.gestalt.assets;

import org.terasology.gestalt.naming.Name;

import java.util.Set;

/**
 * ResolutionStrategy is a filter used when determining what modules providing a resource with a given name to use in a particular module context.
 *
 * @author Immortius
 */
public interface ResolutionStrategy {

    /**
     * @param modules The set of possible modules to resolve
     * @param context The module context of the resolution.
     * @return A Set of modules that are relevant given the context
     */
    Set<Name> resolve(Set<Name> modules, Name context);
}
