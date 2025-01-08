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

package org.terasology.gestalt.module.exceptions;

/**
 * Base exception for all exceptions dealing with modules
 *
 * @author Immortius
 */
public abstract class ModuleException extends Exception {

    /**
     * Creates a ModuleException.
     */
    public ModuleException() {
    }

    /**
     * Creates a ModuleException with message.
     * @param message the message
     */
    public ModuleException(String message) {
        super(message);
    }

    /**
     * Creates a ModuleException with message and cause.
     *
     * @param message   the message
     * @param cause     the cause
     */
    public ModuleException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a ModuleException with cause.
     * @param cause     the cause
     */
    public ModuleException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates a ModuleException message, cause, whether suppresssion is enabled, whether stacktrace can be written.
     *
     * @param message               the message
     * @param cause                 the cause
     * @param enableSuppression     if true, suppression is enabled
     * @param writableStackTrace    if true, stacktrace can be written
     */
    public ModuleException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
