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

package org.terasology.entitysystem.entity.inmemory.pipeline;

import org.terasology.entitysystem.entity.inmemory.EntityState;
import org.terasology.entitysystem.entity.inmemory.EntityStore;
import org.terasology.entitysystem.entity.inmemory.EntitySystemState;
import org.terasology.entitysystem.transaction.pipeline.TransactionContext;
import org.terasology.entitysystem.transaction.pipeline.TransactionInterceptor;

import java.util.ConcurrentModificationException;

/**
 * This TransactionIntercetor verifies the integrity of a commit. It checks that none of the entities involved in the transaction have been altered since the transaction
 * began.
 */
public class VerifyCommitInterceptor implements TransactionInterceptor {

    private EntityStore entityStore;

    public VerifyCommitInterceptor(EntityStore entityStore) {
        this.entityStore = entityStore;
    }

    @Override
    public void handle(TransactionContext context) {
        context.getAttachment(EntitySystemState.class).ifPresent((state) -> {
            checkRevisions(context);
        });
    }

    private EntitySystemState getState(TransactionContext context) {
        return context.getAttachment(EntitySystemState.class).orElseThrow(IllegalStateException::new);
    }

    private void checkRevisions(TransactionContext context) {
        for (EntityState entityState : getState(context).getEntityStates()) {
            if (entityState.getRevision() != entityStore.getEntityRevision(entityState.getId())) {
                throw new ConcurrentModificationException("Entity " + entityState.getId() + " modified outside of transaction");
            }
        }
    }

}
