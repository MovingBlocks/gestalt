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

package org.terasology.module.sandbox;

import java.security.BasicPermission;

/**
 * For permissions relating to Module security.
 *
 * @author Immortius
 * @see java.security.BasicPermission
 * @see java.security.Permission
 * @see java.security.Permissions
 * @see java.security.PermissionCollection
 * @see java.lang.SecurityManager
 */
public class ModuleSecurityPermission extends BasicPermission {

    /**
     * This permission allows permissions to be granted and revoked to the module sandbox
     */
    public static final String UPDATE_ALLOWED_PERMISSIONS = "updateAllowedPermission";

    /**
     * This permission allows the classes and packages available to the module sandbox to be updated
     */
    public static final String UPDATE_API_CLASSES = "updateAPIClasses";

    public ModuleSecurityPermission(String name) {
        super(name);
    }

    public ModuleSecurityPermission(String name, String actions) {
        super(name, actions);
    }
}
