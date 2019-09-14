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

import net.jcip.annotations.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitysystem.component.Component;
import org.terasology.entitysystem.entity.EntityRef;
import org.terasology.entitysystem.event.Event;
import org.terasology.entitysystem.event.EventResult;
import org.terasology.entitysystem.transaction.TransactionManager;

import java.util.ConcurrentModificationException;
import java.util.Set;

/**
 * Basic event system implementation that immediately processes all event on the thread that sent them.
 */
@ThreadSafe
public class ImmediateEventSystem extends AbstractEventSystem {

    private static final Logger logger = LoggerFactory.getLogger(ImmediateEventSystem.class);
    private final EventProcessor eventProcessor;

    /**
     * @param eventProcessor The initial event processor that will execute any events
     */
    public ImmediateEventSystem(TransactionManager transactionManager, EventProcessor eventProcessor) {
        super(transactionManager);
        this.eventProcessor = eventProcessor;
    }

    @Override
    protected void processEvent(Event event, EntityRef entity, Set<Class<? extends Component>> triggeringComponents) {
        if (event.getClass().isAnnotationPresent(Synchronous.class) && getTransactionManager().isActive()) {
            eventProcessor.send(event, entity, triggeringComponents);
        } else {
            boolean completed = false;
            while (!completed) {
                try {
                    getTransactionManager().begin();
                    if (eventProcessor.send(event, entity, triggeringComponents) == EventResult.CANCEL) {
                        getTransactionManager().rollback();
                    } else {
                        getTransactionManager().commit();
                    }
                    completed = true;
                } catch (ConcurrentModificationException e) {
                    logger.debug("Concurrency failure processing event {}, retrying", event, e);
                }
            }
        }
    }

    @Override
    public void processEvents() throws InterruptedException {

    }

    @Override
    public void clearPendingEvents() throws InterruptedException {

    }
}
