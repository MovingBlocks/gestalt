/*
 * Copyright 2016 MovingBlocks
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

package org.terasology.util.collection;

import com.google.common.collect.Lists;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class KahnSorterTest {

    private KahnSorter<Integer> sorter = new KahnSorter<>();

    @Test
    public void trivialSort() {
        assertEquals(Lists.newArrayList(), sorter.sort());
    }

    @Test
    public void noEdgesSort() {
        sorter.addNodes(1, 2, 3);
        assertEquals(Lists.newArrayList(1, 2, 3), sorter.sort());
    }

    @Test
    public void orderedByEdges() {
        sorter.addNodes(1, 2, 3);
        sorter.addEdge(2, 1);
        sorter.addEdge(3, 2);
        assertEquals(Lists.newArrayList(3, 2, 1), sorter.sort());
    }

    @Test(expected=CircularDependencyException.class)
    public void circularDependency() {
        sorter.addNodes(1, 2, 3);
        sorter.addEdge(2, 1);
        sorter.addEdge(1, 2);
        sorter.sort();
    }
}
