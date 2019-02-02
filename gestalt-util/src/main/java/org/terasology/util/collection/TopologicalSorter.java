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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * A TopologicalSorter takes a Directed Acyclic Graph and produces a linear ordering of the nodes,
 * such that each node n appears before every node m where an edge exists from n to m.
 * <p>
 * If a graph contains cycles (e.g. a -> b -> c -> a) then a linear ordering is not possible - instead
 * a {@link CircularDependencyException} will be thrown.
 */
public interface TopologicalSorter<T> {

    /**
     * @param node A node to add to be sorted
     */
    void addNode(T node);

    /**
     * @param nodes A collection of nodes to add to be sorted
     */
    void addNodes(Collection<T> nodes);

    /**
     * @param nodes A series of nodes to add to be sorted
     */
    default void addNodes(T... nodes) {
        addNodes(Arrays.asList(nodes));
    }

    /**
     * An edge top consider when sorting. In the result, fromNode will appear before toNode.
     *
     * @param fromNode The node to appear first in the sorted list
     * @param toNode   The node to appear later in the sorted list
     */
    void addEdge(T fromNode, T toNode);

    /**
     * @return A new, sorted list respecting the ordering of the edges, such that the origin of each edge will appear before the target.
     * @throws CircularDependencyException If there is a circular dependency in the edges.
     */
    List<T> sort();
}
