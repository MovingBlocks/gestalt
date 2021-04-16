// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.gestalt.util.collection;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Test
    public void circularDependency() {
        sorter.addNodes(1, 2, 3);
        sorter.addEdge(2, 1);
        sorter.addEdge(1, 2);
        assertThrows(CircularDependencyException.class, () ->
                sorter.sort()
        );
    }
}
