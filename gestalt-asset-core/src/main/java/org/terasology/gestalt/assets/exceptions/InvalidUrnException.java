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

package org.terasology.gestalt.assets.exceptions;

/**
 * Thrown to indicate a string is not a valid ResourceUrn.
 *
 * @author Immortius
 */
public class InvalidUrnException extends RuntimeException {

    /**
     * creates an exception to be thrown if asset URI was not valid.
     */
    public InvalidUrnException() {
    }

    /**
     * creates an exception to be thrown if asset URI was not valid.
     * @param message the message to be thrown.
     */
    public InvalidUrnException(String message) {
        super(message);
    }

    /**
     * creates an exception to be thrown if asset URI was not valid.
     * @param message the message to be thrown.
     * @param cause throwable causing the exception.
     */
    public InvalidUrnException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * creates an exception to be thrown if asset URI was not valid.
     * @param cause throwable causing the exception.
     */
    public InvalidUrnException(Throwable cause) {
        super(cause);
    }

    /**
     * creates an exception to be thrown if asset URI was not valid.
     * @param message the message to be thrown.
     * @param cause throwable causing the exception.
     * @param enableSuppression true if exception can be suppressed.
     * @param writableStackTrace true if exception stacktrace can be written.
     */
    public InvalidUrnException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
