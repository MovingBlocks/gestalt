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

package org.terasology.entitysystem.transaction.pipeline;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitysystem.transaction.exception.PostCommitException;
import org.terasology.entitysystem.transaction.exception.RollbackException;

import java.util.Optional;

/**
 *
 */
public class TransactionPipeline {

    private static final Logger logger = LoggerFactory.getLogger(TransactionPipeline.class);
    private ListMultimap<TransactionStage, TransactionInterceptor> handlers = ArrayListMultimap.create();

    public void registerInterceptor(TransactionStage stage, TransactionInterceptor interceptor) {
        this.handlers.put(stage, interceptor);
    }

    public void begin(TransactionContext context) {
        processStage(context, TransactionStage.PRE_TRANSACTION);
    }

    public void commit(TransactionContext context) {
        try {
            processStage(context, TransactionStage.PRE_COMMIT);
            try {
                processStage(context, TransactionStage.OBTAIN_LOCKS);
                processStage(context, TransactionStage.VERIFY_COMMIT);
                processStage(context, TransactionStage.PROCESS_COMMIT);
            } finally {
                processStageConsumeExceptions(context, TransactionStage.RELEASE_LOCKS);
            }
        } catch (RuntimeException e) {
            rollback(context);
            throw new RollbackException("Transaction rolled back due to exception", e);
        }
        postCommit(context);
    }

    public void rollback(TransactionContext context) {
        processStageConsumeExceptions(context, TransactionStage.POST_ROLLBACK);
        processStageConsumeExceptions(context, TransactionStage.POST_TRANSACTION);
    }

    private void postCommit(TransactionContext context) {
        Optional<Exception> e1 = processStageConsumeExceptions(context, TransactionStage.UPDATE_INDEXES);
        Optional<Exception> e2 = processStageConsumeExceptions(context, TransactionStage.POST_COMMIT);
        Optional<Exception> e3 = processStageConsumeExceptions(context, TransactionStage.POST_TRANSACTION);
        Exception e = e1.orElseGet(() -> e2.orElseGet(() -> e3.orElse(null)));
        if (e != null) {
            throw new PostCommitException("Error during UPDATE_INDEXES", e);
        }
    }

    private Optional<Exception> processStageConsumeExceptions(TransactionContext context, TransactionStage stage) {
        try {
            processStage(context, stage);
            return Optional.empty();
        } catch (RuntimeException e) {
            return Optional.of(e);
        }
    }

    private void processStage(TransactionContext context, TransactionStage stage) {
        for (TransactionInterceptor interceptor : handlers.get(stage)) {
            interceptor.handle(context);
        }
    }

}
