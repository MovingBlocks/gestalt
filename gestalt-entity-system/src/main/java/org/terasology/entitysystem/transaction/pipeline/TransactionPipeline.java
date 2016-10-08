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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitysystem.transaction.exception.EntitySystemException;

/**
 *
 */
public class TransactionPipeline {

    private static final Logger logger = LoggerFactory.getLogger(TransactionPipeline.class);
    private ListMultimap<TransactionStage, TransactionInterceptor> handlers = ArrayListMultimap.create();

    public void registerInterceptor(TransactionStage stage, TransactionInterceptor interceptor) {
        this.handlers.put(stage, interceptor);
    }

    public void commit(TransactionContext context) {
        boolean rollback;
        try {
            rollback = processStage(context, TransactionStage.PRE_COMMIT, true);
            if (!rollback) {
                rollback = processStage(context, TransactionStage.COMMIT, true);
            }
        } catch (RuntimeException e) {
            rollback(context);
            throw new EntitySystemException("Transaction rolled back due to exception", e);
        }

        if (rollback) {
            rollback(context);
        } else {
            postCommit(context);
        }
    }

    public void rollback(TransactionContext context) {
        processStageConsumeExceptions(context, TransactionStage.POST_ROLLBACK);
        processStageConsumeExceptions(context, TransactionStage.POST_TRANSACTION);
    }

    private void postCommit(TransactionContext context) {
        processStageConsumeExceptions(context, TransactionStage.POST_COMMIT);
        processStageConsumeExceptions(context, TransactionStage.POST_TRANSACTION);
    }

    private void processStageConsumeExceptions(TransactionContext context, TransactionStage stage) {
        try {
            processStage(context, stage, false);
        } catch (RuntimeException e) {
            logger.error("Error during {}", stage, e);
        }
    }

    private boolean processStage(TransactionContext context, TransactionStage stage, boolean stopOnRollback) {
        boolean result = true;
        for (TransactionInterceptor interceptor : handlers.get(stage)) {
            if (!interceptor.handle(context)) {
                result = false;
                if (stopOnRollback) {
                    break;
                }
            }
        }
        return result;
    }

}
