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

package org.terasology.benchmarks.bouncingballs.entity;

import com.google.common.collect.Lists;

import org.terasology.benchmarks.bouncingballs.common.ComponentIterator;
import org.terasology.benchmarks.bouncingballs.common.ComponentStore;

import java.util.Comparator;
import java.util.List;

//public class BruteforceIterator {
//    private List<ComponentStore> componentStores;
//    private ComponentIterator drivingIterator;
//
//    private boolean hasNext = true;
//
//    public BruteforceIterator(ComponentStore ... stores) {
//        this.componentStores = Lists.newArrayList(stores);
//        componentStores.sort(Comparator.comparing(ComponentStore::size));
//        drivingIterator = componentStores.get(0).iterate();
//        findNext();
//    }
//
//    private void findNext() {
//        if (!drivingIterator.next()) {
//            hasNext = false;
//            return;
//        }
//        for (int i = 1; i < componentStores.size(); ++i) {
//            if (componentStores.get(i) == null) {
//                if (!drivingIterator.next()) {
//                    hasNext = false;
//                    break;
//                }
//                i = 0;
//            }
//        }
//    }
//
//    public boolean hasNext() {
//        return hasNext;
//    }
//
//    public int next() {
//        int id = drivingIterator.entityId();
//        findNext();
//        return id;
//    }
//
//}
