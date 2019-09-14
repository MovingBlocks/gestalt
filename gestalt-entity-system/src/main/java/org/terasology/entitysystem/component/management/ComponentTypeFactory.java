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

package org.terasology.entitysystem.component.management;

import android.support.annotation.NonNull;

import org.terasology.entitysystem.component.Component;

/**
 * Interface for generating ComponentTypes from a component class
 */
@FunctionalInterface
public interface ComponentTypeFactory {

    /**
     * @param type The class of component to generate a ComponentType for
     * @param <T>  The class of component to generate a ComponentType for
     * @return A generated component type
     * @throws ComponentTypeGenerationException If there is a problem generating a component type
     */
    @NonNull
    <T extends Component<T>> ComponentType<T> createComponentType(Class<T> type);
}
