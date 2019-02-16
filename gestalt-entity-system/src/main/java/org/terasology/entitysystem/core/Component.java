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
 * This is the base interface for all components. To define a component, create a new interface extending this interface and add properties (getter and setters) for any data
 * the component should contain.
 */
public interface Component {

    /**
     * @return The interface type of the component
     */
    Class<? extends Component> getType();

}
