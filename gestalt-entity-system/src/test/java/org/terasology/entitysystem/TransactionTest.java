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

package org.terasology.entitysystem;

import org.junit.Test;
import org.terasology.entitysystem.entity.EntityTransaction;
import org.terasology.entitysystem.event.EventSystem;
import org.terasology.entitysystem.stubs.TestEvent;
import org.terasology.entitysystem.stubs.TestSynchEvent;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 *
 */
public class TransactionTest {

    private static final long ENTITY_ID = 12;
    private TestSynchEvent synchronousEvent = new TestSynchEvent("Test");
    private TestEvent asynchronousEvent = new TestEvent("Test");

    private EntityTransaction entityTransaction = mock(EntityTransaction.class);
    private EventSystem eventSystem = mock(EventSystem.class);



    @Test
    public void synchronousEventProcessedImmediately() {
        Transaction transaction = new Transaction(entityTransaction, eventSystem);
        transaction.send(synchronousEvent, ENTITY_ID);
        verify(eventSystem).send(synchronousEvent, ENTITY_ID, transaction);
    }

    @Test
    public void synchronousEventProcessedAfterCommit() {
        Transaction transaction = new Transaction(entityTransaction, eventSystem);
        transaction.send(asynchronousEvent, ENTITY_ID);
        verifyNoMoreInteractions(eventSystem);
        transaction.commit();
        verify(eventSystem).send(asynchronousEvent, ENTITY_ID);
    }

    @Test
    public void synchronousEventNotProcessedAfterRollback() {
        Transaction transaction = new Transaction(entityTransaction, eventSystem);
        transaction.send(asynchronousEvent, ENTITY_ID);
        verifyNoMoreInteractions(eventSystem);
        transaction.rollback();
        verifyNoMoreInteractions(eventSystem);
        transaction.commit();
        verifyNoMoreInteractions(eventSystem);
    }

    @Test
    public void synchronousEventNotProcessedAgainAfterCommit() {
        Transaction transaction = new Transaction(entityTransaction, eventSystem);
        transaction.send(asynchronousEvent, ENTITY_ID);
        verifyNoMoreInteractions(eventSystem);
        transaction.commit();
        verify(eventSystem).send(asynchronousEvent, ENTITY_ID);
        transaction.commit();
        verifyNoMoreInteractions(eventSystem);
    }
}
