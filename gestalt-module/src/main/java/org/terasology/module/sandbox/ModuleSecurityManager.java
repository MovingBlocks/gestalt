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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.module.Module;

import java.security.Permission;
import java.util.List;
import java.util.Map;

/**
 * ModuleSecurityManager establishes and enforces a sandbox environment for modules. Modules are restricted to make use of specific classes, packages and permissions
 * - they are prevented from accessing anything else.
 * <p>
 * The following access may be granted:
 * </p>
 * <ul>
 * <li>Access to a specific class (API class)</li>
 * <li>Access to a specific package (API package)</li>
 * <li>Globally allow a permission</li>
 * <li>Allow a permission if requested from a specific non-module class or package</li>
 * <li>Give a non-module package all permissions</li>
 * </ul>
 * <p>
 * When checking permissions, only the stack down to the calling module (if any) is considered. This means that a module cannot exploit a package with higher
 * permissions.
 * </p>
 * <p>
 * AccessController.doPrivileged() is fully supported by this system, so non-module code can use this to avoid needing to be explicitly registered as allowing a permission.
 * </p>
 *
 * @author Immortius
 * @see ModuleClassLoader
 */
public class ModuleSecurityManager extends SecurityManager implements PermissionProviderFactory {

    public static final Permission UPDATE_ALLOWED_PERMISSIONS = new ModuleSecurityPermission(ModuleSecurityPermission.UPDATE_ALLOWED_PERMISSIONS);
    public static final Permission UPDATE_API_CLASSES = new ModuleSecurityPermission(ModuleSecurityPermission.UPDATE_API_CLASSES);
    public static final String BASE_PERMISSION_SET = "";
    private static final Logger logger = LoggerFactory.getLogger(ModuleSecurityManager.class);

    private final Map<String, PermissionSet> permissionSets = Maps.newHashMap();

    private ThreadLocal<Boolean> calculatingPermission = new ThreadLocal<>();

    public ModuleSecurityManager() {
        permissionSets.put(BASE_PERMISSION_SET, new PermissionSet());
    }

    public PermissionSet getBasePermissionSet() {
        return getPermissionSet(BASE_PERMISSION_SET);
    }

    public PermissionSet getPermissionSet(String name) {
        return permissionSets.get(name);
    }

    public void addPermissionSet(String name, PermissionSet permissionSet) {
        this.permissionSets.put(name, permissionSet);
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
     * Checks whether a permission is allowed under the current module context. The process for this is:
     * <ol>
     * <li>If the permission is globally allowed, then permission is granted</li>
     * <li>Determine if a module is involved in the stack. If not, permission is granted</li>
     * <li>If a module is involved, determine whether it is calling through an API class that grants the necessary permission</li>
     * <li>If not, permission denied</li>
     * </ol>
     *
     * @param perm The permission under question
     */
    private boolean checkModuleDeniedAccess(Permission perm) {

        if (calculatingPermission.get() != null) {
            return false;
        }

        calculatingPermission.set(true);

        try {
            Class[] stack = getClassContext();
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
    private boolean checkAPIPermissionsFor(Permission permission, int moduleDepth, Class[] stack, PermissionProvider permissionProvider) {
        for (int i = moduleDepth - 1; i >= 0; i--) {
            if (permissionProvider.isPermitted(permission, stack[i])) {
                return true;
            }
        }
        return false;
    }


    @Override
    public PermissionProvider createPermissionProviderFor(Module module) {
        List<PermissionSet> grantedPermissionSets = Lists.newArrayList();
        grantedPermissionSets.add(permissionSets.get(BASE_PERMISSION_SET));
        for (String permissionSetId : module.getRequiredPermissions()) {
            PermissionSet set = permissionSets.get(permissionSetId);
            if (set != null) {
                grantedPermissionSets.add(set);
            } else {
                logger.warn("Module '{}' requires unknown permission '{}'", module, permissionSetId);
            }
        }
        return new SetUnionPermissionProvider(grantedPermissionSets);
    }
}
