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

package org.terasology.module.sandbox;

import java.security.Permission;

/**
 * <p>ModuleSecurityManager enforces permission access for modules.</p>
 * <p>The actual permissions are determined by the PermissionProvider associated with the module</p>
 * <p>
 * When checking permissions, only the stack down to the calling module (if any) is considered. This means that a module cannot exploit a package with higher
 * permissions.
 * </p>
 * <p>
 * AccessController.doPrivileged() is fully supported by this system, so non-module code can use this to avoid needing to be explicitly registered as allowing a permission
 * to modules using it, if the code is intended to run at the engine's security level.
 *
 * @author Immortius
 * @see ModuleClassLoader
 */
public class ModuleSecurityManager extends SecurityManager {

    public static final Permission UPDATE_ALLOWED_PERMISSIONS = new ModuleSecurityPermission(ModuleSecurityPermission.UPDATE_ALLOWED_PERMISSIONS);
    public static final Permission UPDATE_API_CLASSES = new ModuleSecurityPermission(ModuleSecurityPermission.UPDATE_API_CLASSES);

    private ThreadLocal<Boolean> calculatingPermission = new ThreadLocal<>();

    public ModuleSecurityManager() {
    }

    @Override
    public void checkPermission(Permission perm) {
        if (checkModuleDeniedAccess(perm)) {
            super.checkPermission(perm);
        }
    }

    @Override
    public void checkPermission(Permission perm, Object context) {
        if (checkModuleDeniedAccess(perm)) {
            super.checkPermission(perm);
        }
    }

    /**
     * Checks whether a permission is allowed under the current module context.
     *
     * @param perm The permission under question
     * @return Whether the permission is denied
     */
    private boolean checkModuleDeniedAccess(Permission perm) {

        if (calculatingPermission.get() != null) {
            return false;
        }

        calculatingPermission.set(true);

        try {
            Class<?>[] stack = getClassContext();
            for (int i = 0; i < stack.length; ++i) {
                ClassLoader owningLoader = stack[i].getClassLoader();
                if (owningLoader != null && owningLoader instanceof ModuleClassLoader) {
                    return !checkAPIPermissionsFor(perm, i, stack, ((ModuleClassLoader) owningLoader).getPermissionProvider());
                }
            }
        } finally {
            calculatingPermission.set(null);
        }
        return true;
    }

    /**
     * Checks the stack down to the first module to see if the given permission is allowed to be triggered from a module context.
     *
     * @param permission  The permission being checked
     * @param moduleDepth The depth of the first module class
     * @param stack       The classes involved in the current stack
     * @return Whether the permission has been granted to any of the API classes involved.
     */
    private boolean checkAPIPermissionsFor(Permission permission, int moduleDepth, Class<?>[] stack, PermissionProvider permissionProvider) {
        for (int i = moduleDepth - 1; i >= 0; i--) {
            if (permissionProvider.isPermitted(permission, stack[i])) {
                return true;
            }
        }
        return false;
    }


}
