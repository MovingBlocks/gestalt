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

package org.terasology.entitysystem.transaction;

import com.google.common.base.Preconditions;
import com.google.common.collect.Queues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitysystem.transaction.pipeline.TransactionContext;
import org.terasology.entitysystem.transaction.pipeline.TransactionPipeline;

import java.util.Deque;

/**
 * Transaction handling for a single thread.
 */
class ThreadTransaction implements Transaction {

    private static final Logger logger = LoggerFactory.getLogger(ThreadTransaction.class);

    private final Deque<TransactionContext> transactionState = Queues.newArrayDeque();
    private final TransactionPipeline pipeline;

    ThreadTransaction(TransactionPipeline pipeline) {
        this.pipeline = pipeline;
    }

    public boolean isActive() {
        return !transactionState.isEmpty();
    }

    @Override
    public TransactionContext getContext() {
        Preconditions.checkState(isActive(), "No active transaction");
        return transactionState.peek();
    }

    public void begin() {
        TransactionContext context = new TransactionContext();
        transactionState.push(context);
        pipeline.begin(context);
    }

    @SuppressWarnings("unchecked")
    public void commit() {
        Preconditions.checkState(isActive(), "No active transaction to commit");
        TransactionContext context = transactionState.pop();
        pipeline.commit(context);
    }

    public void rollback() {
        Preconditions.checkState(isActive(), "No active transaction to rollback");
        TransactionContext context = transactionState.pop();
        pipeline.rollback(context);
    }
}
