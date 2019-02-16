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

package org.terasology.entitysystem.entity.inmemory.pipeline;

import org.terasology.entitysystem.core.NullEntityRef;
import org.terasology.entitysystem.entity.inmemory.EntitySystemState;
import org.terasology.entitysystem.entity.inmemory.NewEntityState;
import org.terasology.entitysystem.transaction.pipeline.TransactionContext;
import org.terasology.entitysystem.transaction.pipeline.TransactionInterceptor;

/**
 * This TransactionInterceptor changes any proxy entity refs for new entities to point to {@link NullEntityRef} instead. This cleans up after a rollback.
 */
public class WipeNewEntityInterceptor implements TransactionInterceptor {
    @Override
    public void handle(TransactionContext context) {
        context.getAttachment(EntitySystemState.class).ifPresent((state) -> {
            for (NewEntityState entityRef : state.getNewEntities()) {
                entityRef.setActualEntity(NullEntityRef.get());
            }
            state.getNewEntities().clear();
        });
    }
}
