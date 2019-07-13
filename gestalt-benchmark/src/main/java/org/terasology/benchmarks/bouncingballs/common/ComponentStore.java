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

package org.terasology.benchmarks.bouncingballs.common;

import org.terasology.entitysystem.core.Component;

public interface ComponentStore<T extends Component<T>> {

    boolean get(int entityId, T into);

    void set(int entityId, T component);

    int size();

    ComponentIterator<T> iterate();

    ComponentSpliterator<T> spliterate();

    void remove(int id);

    Class<T> getType();

    void extend(int capacity);

}
