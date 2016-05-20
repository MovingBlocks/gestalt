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

package org.terasology.entitysystem.entity;

/**
 * An interface for listeners on transaction events. This allows additional behaviors to be hooked into transactions.
 * <p>
 * Events will be sent on the thread starting a transaction, so a TransactionEventListener must be thread safe in the presence of multiple threads.
 */
public interface TransactionEventListener {

    /**
     * Called after an new transaction has begun
     */
    default void onBegin() {};

    /**
     * Called after a transaction has been successfully committed.
     */
    default void onCommit() {};

    /**
     * Called after a transaction has been rolled back.
     */
    default void onRollback() {}
}
