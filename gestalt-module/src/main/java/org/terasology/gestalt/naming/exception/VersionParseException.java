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

package org.terasology.gestalt.naming.exception;

/**
 * Exception when a version string fails to be parsed.
 *
 * @author Immortius
 */
public class VersionParseException extends RuntimeException {

    /**
     * Default constructor.
     */
    public VersionParseException() {
    }

    /**
     * Constructor with message.
     * @param message   the message
     */
    public VersionParseException(String message) {
        super(message);
    }

    /**
     * Constructor with message and throwable.
     * @param message   the message
     * @param cause     the cause
     */
    public VersionParseException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor with cause.
     * @param cause     the cause
     */
    public VersionParseException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor with message, cause, if suppression is enabled, and if stack trace can be written.
     * @param message               the message
     * @param cause                 the cause
     * @param enableSuppression     true if exception can be suppressed
     * @param writableStackTrace    true if exception stacktrace can be written
     */
    public VersionParseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
