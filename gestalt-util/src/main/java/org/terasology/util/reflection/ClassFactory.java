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

package org.terasology.util.reflection;

import java.util.Optional;

/**
 * Interface for Class factories. These generically generate requested classes.
 *
 * @author Immortius
 */
public interface ClassFactory {
    /**
     * @param type The type to instantiate.
     * @param <T>  The type to return the instantiated object as
     * @return An optional that contains the instantiated object if successful
     */
    <T> Optional<T> instantiateClass(Class<? extends T> type);
}
