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

package org.terasology.entitysystem.core;

/**
 * A component is an element that can be added to an entity. A component holds data, and implies a feature or behavior of the entity.
 * <p>
 * All components should be concrete final objects implementing component and its methods, and having both an empty constructor and a copy method as a minimum.
 * If a copy constructor is available it will be used too.
 * Components should not inherit other components, and there is generally no reason to have common interfaces or abstract base types shared by components -
 * such desires generally suggest that there is another component that should be created.
 */
public interface Component {

    /**
     * @return Whether the component has been altered within the current context
     */
    boolean isDirty();

    /**
     * Sets whether the component is dirty. Generally shouldn't need to be called, used by
     * the entity manager to reset dirty as needed.
     * @param dirty Whether the component is dirty
     */
    void setDirty(boolean dirty);

    /**
     * Copies the values from another component. This is expected to be of the same type.
     * @param other The component to copy
     */
    void copy(Component other);
}
