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

/**
 *
 */
public enum TransactionStage {
    /**
     * This stage occurs before commit is attempted. At this stage the transactional state can be freely modified.
     */
    PRE_COMMIT,
    /**
     * This stage is where the commit is attempted. At this point the transactional state is merged into the entity system and becomes visible to future
     * transactions.
     */
    COMMIT,
    /**
     * This stage occurs after the commit succeeds. At this stage changes to the transactional state will no longer be applied to the entity system. This can be used
     * to run actions in response to the successful commit.
     */
    POST_COMMIT,
    /**
     * This stage occurs after the commit fails. The transactional state reflects the intended changes, but they will not occurred. This can be used to run
     * actions in response to the failed commit
     */
    POST_ROLLBACK,
    /**
     * This stage occurs after either POST_COMMIT or POST_ROLLBACK, depending on whether the transaction succeeded or failed. It can be use to run any common actions
     * to cleanup after a transaction.
     */
    POST_TRANSACTION
}
