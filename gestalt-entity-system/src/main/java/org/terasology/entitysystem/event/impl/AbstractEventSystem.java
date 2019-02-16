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

package org.terasology.entitysystem.event.impl;

import com.google.common.collect.ImmutableSet;

import org.terasology.entitysystem.core.Component;
import org.terasology.entitysystem.core.EntityRef;
import org.terasology.entitysystem.event.Event;
import org.terasology.entitysystem.event.EventSystem;
import org.terasology.entitysystem.event.Synchronous;
import org.terasology.entitysystem.transaction.TransactionManager;
import org.terasology.entitysystem.transaction.pipeline.TransactionStage;

import java.util.Set;

/**
 *
 */
public abstract class AbstractEventSystem implements EventSystem {

    private TransactionManager transactionManager;

    public AbstractEventSystem(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
        transactionManager.getPipeline().registerInterceptor(TransactionStage.PRE_TRANSACTION, new InitialiseEventState());
        transactionManager.getPipeline().registerInterceptor(TransactionStage.POST_COMMIT, new CommitEvents(this));
    }

    protected TransactionManager getTransactionManager() {
        return transactionManager;
    }

    @Override
    public final void send(Event event, EntityRef entity) {
        send(event, entity, ImmutableSet.of());
    }

    @Override
    public final void send(Event event, EntityRef entity, Set<Class<? extends Component>> triggeringComponents) {
        if (event.getClass().isAnnotationPresent(Synchronous.class) || !transactionManager.isActive()) {
            processEvent(event, entity, triggeringComponents);
        } else {
            transactionManager.getContext().getAttachment(EventState.class).ifPresent((state) -> {
                state.getPendingEvents().add(new PendingEventInfo(event, entity, triggeringComponents));
            });
        }
    }

    protected abstract void processEvent(Event event, EntityRef entity, Set<Class<? extends Component>> triggeringComponents);

}
