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

package org.terasology.module.dependencyResolution;

/**
 * OptionalResolutionStrategy determines how optional dependencies are resolved by the dependency resolver.
 *
 * @author Immortius
 */
public enum OptionalResolutionStrategy {
    /**
     * Causes optional dependencies to be treated as mandatory dependencies.
     */
    FORCE_INCLUDE(true, true),

    /**
     * If an optional dependency is available it will be included, but missing optional dependency will not cause resolution failure.
     */
    INCLUDE_IF_AVAILABLE(false, true),

    /**
     * Optional dependencies will not be included unless otherwise required, but if present the version constraints will be applied
     */
    INCLUDE_IF_REQUIRED(false, false);

    private final boolean required;
    private final boolean desired;

    OptionalResolutionStrategy(boolean required, boolean desired) {
        this.required = required;
        this.desired = desired;
    }

    /**
     * @return Whether an optional dependency must be provided
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * @return Whether an optional dependency will be included if available
     */
    public boolean isDesired() {
        return desired;
    }
}
