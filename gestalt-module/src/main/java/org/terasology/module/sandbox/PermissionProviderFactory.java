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

package org.terasology.module.sandbox;

import org.terasology.module.Module;

import java.util.function.Predicate;

/**
 * Interface for factories that produce permission providers for modules.
 *
 * @author Immortius
 */
public interface PermissionProviderFactory {

    /**
     * @param module The module to create a permission provider for.
     * @return A permission provider suitable for the given module
     */
    PermissionProvider createPermissionProviderFor(Module module, Predicate<Class<?>> classpathModuleClasses);
}
