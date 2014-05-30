/*
 * Copyright 2014 MovingBlocks
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

package org.terasology.util;

import com.google.common.collect.Sets;

import java.util.Arrays;
import java.util.Set;

/**
 * Utility class for methods to help with the use of vararg parameters
 *
 * @author Immortius
 */
public final class Varargs {

    private Varargs() {
    }

    /**
     * Combines a single value and array into a set. Iteration of the set maintains the order of the times.
     * This is intended to aid methods using the mandatory-first optional-additional varargs trick
     *
     * @param first      The first, single value
     * @param additional Any additional values
     * @return A set of the combined values
     */
    @SafeVarargs
    public static <T> Set<T> combineToSet(T first, T... additional) {
        Set<T> full = Sets.newLinkedHashSetWithExpectedSize(additional.length + 1);
        full.add(first);
        full.addAll(Arrays.asList(additional));
        return full;
    }
}
