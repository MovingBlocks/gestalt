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

package org.terasology.entitysystem.transaction.pipeline;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.terasology.entitysystem.transaction.references.NewEntityRef;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 *
 */
public class TransactionState {
    private Map<Long, TransactionalEntityState> existingEntityState = Maps.newLinkedHashMap();
    private List<NewEntityRef> newEntities = Lists.newArrayList();

    public Collection<TransactionalEntityState> getEntityStates() {
        return Collections.unmodifiableCollection(existingEntityState.values());
    }

    public Optional<TransactionalEntityState> getStateFor(long entityId) {
        return Optional.ofNullable(existingEntityState.get(entityId));
    }

    public void addState(TransactionalEntityState state) {
        existingEntityState.put(state.getId(), state);
    }

    public Set<Long> getInvolvedEntityIds() {
        return Collections.unmodifiableSet(existingEntityState.keySet());
    }

    public List<NewEntityRef> getNewEntities() {
        return newEntities;
    }
}
