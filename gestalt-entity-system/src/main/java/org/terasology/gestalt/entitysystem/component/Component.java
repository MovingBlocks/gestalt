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

package org.terasology.gestalt.entitysystem.component;

import org.terasology.context.annotation.IndexInherited;

/**
 * A component is an element that can be added to an entity. A component holds data, and implies a feature or behavior of the entity.
 * <p>
 * All components should be concrete final objects implementing component and its methods, and having both an empty constructor and a copy method as a minimum.
 * If a copy constructor is available it will be used too.
 * <p>
 * Component implementations should be final and not inherit other Component implementations. Entity Systems are a composition based approach -
 * the behaviour of an entity is determined by its components. If there is shared behavior between components instead the shared element can be pulled apart to
 * produce a separate component that works in tandem with the components it was extracted from. The one case you might consider inheritance would be where
 * you have two components with similar configuration requirements but different behavior. Note that the entity system does not take into consideration
 * inheritance - you cannot retrieve components via a super type - so and shared inheritance is much like private inheritance in C++.
 */
@IndexInherited
public interface Component<T extends Component> {

    /**
     * Copies the values from another component. This is expected to be of the same type.
     * @param other The component to copy
     */
    void copy(T other);
}
