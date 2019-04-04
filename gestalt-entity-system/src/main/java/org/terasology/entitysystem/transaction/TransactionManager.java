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

package org.terasology.entitysystem.transaction;

import org.terasology.entitysystem.transaction.pipeline.TransactionContext;
import org.terasology.entitysystem.transaction.pipeline.TransactionPipeline;

/**
 *
 */
public class TransactionManager implements Transaction {

    private final TransactionPipeline pipeline = new TransactionPipeline();

    // Note: using the old version ThreadLocal initial value technique as the new version is not
    // available under older Android API versions
    private final ThreadLocal<Transaction> transactions = new ThreadLocal<Transaction>() {
        @Override
        protected Transaction initialValue() {
            return new ThreadTransaction(pipeline);
        }
    };

    public TransactionPipeline getPipeline() {
        return pipeline;
    }

    @Override
    public boolean isActive() {
        return transactions.get().isActive();
    }

    @Override
    public TransactionContext getContext() {
        return transactions.get().getContext();
    }

    @Override
    public void begin() {
        transactions.get().begin();
    }

    @Override
    public void rollback() {
        transactions.get().rollback();
    }

    @Override
    public void commit() {
        transactions.get().commit();
    }
}
