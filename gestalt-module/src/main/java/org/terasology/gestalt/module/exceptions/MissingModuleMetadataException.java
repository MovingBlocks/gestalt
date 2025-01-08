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
 * Exception for when metadata cannot be resolved for a module
 */
public class MissingModuleMetadataException extends RuntimeException {

    /**
     * Constructor.
     */
    public MissingModuleMetadataException() {
    }

    /**
     * Constructor with message.
     *
     * @param s the message.
     */
    public MissingModuleMetadataException(String s) {
        super(s);
    }

    /**
     * Constructor with message and throwable.
     * @param s             the message
     * @param throwable     the throwable
     */
    public MissingModuleMetadataException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
