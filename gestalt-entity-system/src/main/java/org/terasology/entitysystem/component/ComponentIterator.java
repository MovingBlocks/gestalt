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

package org.terasology.entitysystem.component;

import org.terasology.entitysystem.component.Component;

/**
 * An iterator over components. This doesn't use the standard iterator interface because of the
 * component copy behavior
 *
 * @param <T> The type of component being iterated over
 */
public interface ComponentIterator<T extends Component<T>> {

    /**
     * Populates the provided component with the contents of the next component
     *
     * @return Whether there was a component available to copy
     */
    boolean next();

    /**
     * @return The id of the last returned entity
     */
    int getEntityId();

    /**
     * @param component Populates the given component with the component for the current entity
     */
    void getComponent(Component<T> component);

}
