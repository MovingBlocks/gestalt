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

/**
 *
 */
public enum TransactionStage {
    /**
     * This stage occurs when a transaction begins. It should be used to initialise state in the transaction context.
     */
    PRE_TRANSACTION,
    /**
     * This stage occurs prior to the commit being attempted. At this stage the transactional state can be freely modified.
     */
    PRE_COMMIT,
    /**
     * Start of the actual commit. Used to obtain locks on any resources that may be needed
     */
    OBTAIN_LOCKS,
    /**
     * After locks are obtained, this is the ideal point to verify that the commit will succeed, and trigger a rollback if it won't.
     */
    VERIFY_COMMIT,
    /**
     * This stage for the main processing of a commit. At this point the transactional state is merged into the entity system and becomes visible to future
     * transactions. While rollbacks can be triggered at this point, it should be noted that other systems involved in the transaction may have done their commit work already.
     */
    PROCESS_COMMIT,
    /**
     * Called at the end of a commit process, whether it succeeded or failed. Should be used to release locks on any resources involved in the commit. Note that it will be
     * called if an error occurs during the processing of OBTAIN_LOCKS, so the lock state may be incomplete.
     */
    RELEASE_LOCKS,
    /**
     * This stage occurs after a commit succeeds, but before POST_COMMIT. This is the stage to to update any indexes or other information in response to the transaction, before
     * doing further processing in response to the commit.
     */
    UPDATE_INDEXES,
    /**
     * This stage occurs after a commit succeeds. At this stage changes to the transactional state will no longer be applied to the entity system. This can be used
     * to run actions in response to the successful commit.
     */
    POST_COMMIT,
    /**
     * This stage occurs after a commit fails. The transactional state reflects the intended changes, but they will not occurred. This can be used to run
     * actions in response to the failed commit
     */
    POST_ROLLBACK,
    /**
     * This stage occurs after either POST_COMMIT or POST_ROLLBACK, depending on whether the transaction succeeded or failed. It can be use to run any common actions
     * to cleanup after a transaction.
     */
    POST_TRANSACTION
}
