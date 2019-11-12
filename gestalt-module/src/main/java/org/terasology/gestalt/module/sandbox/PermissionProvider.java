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

package org.terasology.gestalt.module.sandbox;

import java.security.Permission;

/**
 * Provides checks for what classes and permissions are permitted to a module/ModuleClassLoader.
 *
 * @author Immortius
 */
public interface PermissionProvider {

    /**
     * @param type The class to check
     * @return Whether access to the given class is permitted
     */
    boolean isPermitted(Class<?> type);

    /**
     * @param permission The permission to check
     * @param context    The type invoking the permission check
     * @return Whether access to the given permission is permitted
     */
    boolean isPermitted(Permission permission, Class<?> context);
}
