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

package org.terasology.entitysystem.transaction.exception;

/**
 * This exception is thrown if an error occurs during post commit processing. This means the commit has succeeded, but something went wrong in the subsequent processing.
 */
public class PostCommitException extends EntitySystemException {

    public PostCommitException() {
    }

    public PostCommitException(String message) {
        super(message);
    }

    public PostCommitException(String message, Throwable cause) {
        super(message, cause);
    }

    public PostCommitException(Throwable cause) {
        super(cause);
    }

}
