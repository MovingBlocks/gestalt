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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * An implementation of TopologicalSorter that uses the Kahn algorithm.
  */
public class KahnSorter<T> implements TopologicalSorter<T> {

    private Set<T> openNodes = Sets.newLinkedHashSet();

    private ListMultimap<T, T> dependentsLookup = ArrayListMultimap.create();
    private TObjectIntMap<T> dependencyCount = new TObjectIntHashMap<>();

    @Override
    public void addNode(T node) {
        openNodes.add(node);
    }

    @Override
    public void addNodes(Collection<T> nodes) {
        openNodes.addAll(nodes);
    }

    @Override
    public void addEdge(T fromNode, T toNode) {
        openNodes.remove(toNode);
        dependentsLookup.put(fromNode, toNode);
        dependencyCount.adjustOrPutValue(toNode, 1, 1);
    }

    @Override
    public List<T> sort() {
        List<T> resultList = Lists.newArrayList();
        while (!openNodes.isEmpty()) {
            T node = openNodes.iterator().next();
            openNodes.remove(node);
            resultList.add(node);

            for (T dependent : dependentsLookup.removeAll(node)) {
                int dependencies = dependencyCount.adjustOrPutValue(dependent, -1, 0);
                if (dependencies == 0) {
                    openNodes.add(dependent);
                }
            }
        }

        if (!dependentsLookup.isEmpty()) {
            throw new CircularDependencyException("Could not sort nodes, one or more circular dependencies exist involving: " + dependentsLookup.keySet());
        }

        return resultList;
    }
}
