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

package org.terasology.gestalt.entitysystem.event;

/**
 * Possible results from processing an event
 */
public enum EventResult {
    /**
     * Signals that event processing should continue.
     */
    CONTINUE,

    /**
     * Signals that event processing should halt, but be considered successfully complete.
     */
    COMPLETE,

    /**
     * Signals that event processing should halt, and should be considered unsuccessful.
     */
    CANCEL
}
