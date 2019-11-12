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

package org.terasology.gestalt.module.predicates;

import com.google.common.base.Predicate;

import org.terasology.gestalt.module.ModuleEnvironment;
import org.terasology.gestalt.naming.Name;

import java.util.Objects;

/**
 * Predicate for filtering classes to those from a specific module.
 *
 * @author Immortius
 */
public class FromModule implements Predicate<Class<?>> {

    private final ModuleEnvironment environment;
    private final Name moduleId;

    public FromModule(ModuleEnvironment environment, Name moduleId) {
        this.environment = environment;
        this.moduleId = moduleId;
    }

    @Override
    public boolean apply(Class<?> input) {
        return Objects.equals(environment.getModuleProviding(input), moduleId);
    }
}
