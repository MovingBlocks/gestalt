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

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an event as synchronous - to run immediately when sent, rather than being queued to run later.
 * Note: Care should be taken around the use of synchronous events, as components the caller has loaded are
 * not visible to the handlers of the synchronous event - if the handlers modify a component the caller
 * has loaded then the caller won't see those changes unless it reloads the component (and may save
 * over those changes). Recommendation is the caller commits any changes that may be relevant before
 * sending a synchronous event, and that the receivers do not modify anything
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD, ElementType.TYPE})
@Inherited
public @interface Synchronous {
}
