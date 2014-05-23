/*
 * Copyright 2014 MovingBlocks
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

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Permission;
import java.util.Iterator;
import java.util.Set;

/**
 * ModuleSecurityManager establishes and enforces a sandbox environment for modules. Modules are restricted to make use of specific classes, packages and permissions
 * - they are prevented from accessing anything else.
 *
 * The following access may be granted:
 * <ul>
 *     <li>Access to a specific class (API class)</li>
 *     <li>Access to a specific package (API package)</li>
 *     <li>Globally allow a permission</li>
 *     <li>Allow a permission if requested from a specific non-module class or package</li>
 *     <li>Give a non-module package all permissions</li>
 * </ul>
 *
 * When checking permissions, only the stack down to the calling module (if any) is considered. This means that a module cannot exploit a package with higher
 * permissions.
 *
 * AccessController.doPrivileged() is fully supported by this system, so non-module code can use this to avoid needing to be explicitly registered as allowing a permission.
 *
 * @see ModuleClassLoader
 * @author Immortius
 */
public class ModuleSecurityManager extends SecurityManager implements APIProvider {

    private static final Logger logger = LoggerFactory.getLogger(ModuleSecurityManager.class);
    private static final Permission UPDATE_ALLOWED_PERMISSIONS = new ModuleSecurityPermission(ModuleSecurityPermission.UPDATE_ALLOWED_PERMISSIONS);
    private static final Permission UPDATE_API_CLASSES = new ModuleSecurityPermission(ModuleSecurityPermission.UPDATE_API_CLASSES);

    private Set<Class> apiClasses = Sets.newHashSet();
    private Set<String> apiPackages = Sets.newHashSet();
    private Set<String> fullPrivilegePackages = Sets.newHashSet();

    private Set<Class<? extends Permission>> globallyAllowedPermissionsTypes = Sets.newHashSet();
    private Set<Permission> globallyAllowedPermissionsInstances = Sets.newHashSet();
    private SetMultimap<Class<? extends Permission>, Class> allowedPermissionsTypes = HashMultimap.create();
    private SetMultimap<Permission, Class> allowedPermissionInstances = HashMultimap.create();
    private SetMultimap<Class<? extends Permission>, String> allowedPackagePermissionsTypes = HashMultimap.create();
    private SetMultimap<Permission, String> allowedPackagePermissionInstances = HashMultimap.create();

    private ThreadLocal<Boolean> calculatingPermission = new ThreadLocal<>();

    public ModuleSecurityManager() {
    }

    /**
     * Registers a global permission that all modules are granted
     *
     * @param permission The class of permission to grant
     */
    public void grantPermission(Class<? extends Permission> permission) {
        Preconditions.checkNotNull(permission);
        if (System.getSecurityManager() != null) {
            System.getSecurityManager().checkPermission(UPDATE_ALLOWED_PERMISSIONS);
        }
        globallyAllowedPermissionsTypes.add(permission);
        logger.debug("Globally granted permission '{}'", permission);
    }

    /**
     * Registers a global permission that all modules are granted
     *
     * @param permission The permission to grant
     */
    public void grantPermission(Permission permission) {
        Preconditions.checkNotNull(permission);
        if (System.getSecurityManager() != null) {
            System.getSecurityManager().checkPermission(UPDATE_ALLOWED_PERMISSIONS);
        }
        globallyAllowedPermissionsInstances.add(permission);
        logger.debug("Globally granted permission '{}'", permission);
    }

    /**
     * Registers a permission that modules are granted when working (directly or indirectly) through the given apiType
     *
     * @param apiType The type that requires the permission
     * @param permission The class of permission to grant
     */
    public void grantPermission(Class apiType, Class<? extends Permission> permission) {
        Preconditions.checkNotNull(apiType);
        Preconditions.checkNotNull(permission);
        if (System.getSecurityManager() != null) {
            System.getSecurityManager().checkPermission(UPDATE_ALLOWED_PERMISSIONS);
        }
        allowedPermissionsTypes.put(permission, apiType);
        logger.debug("Granted permission '{}' to '{}'", permission, apiType);
    }

    /**
     * Registers a permission that modules are granted when working (directly or indirectly) through the given apiType
     *
     * @param apiType The type that requires the permission
     * @param permission The permission to grant
     */
    public void grantPermission(Class apiType, Permission permission) {
        Preconditions.checkNotNull(apiType);
        Preconditions.checkNotNull(permission);
        if (System.getSecurityManager() != null) {
            System.getSecurityManager().checkPermission(UPDATE_ALLOWED_PERMISSIONS);
        }
        allowedPermissionInstances.put(permission, apiType);
        logger.debug("Granted permission '{}' to '{}'", permission, apiType);
    }

    /**
     * Registers a permission that modules are granted when working (directly or indirectly) through the given package
     *
     * @param packageName The package that requires the permission
     * @param permission The class of permission to grant
     */
    public void grantPermission(String packageName, Class<? extends Permission> permission) {
        Preconditions.checkNotNull(packageName);
        Preconditions.checkNotNull(permission);
        if (System.getSecurityManager() != null) {
            System.getSecurityManager().checkPermission(UPDATE_ALLOWED_PERMISSIONS);
        }
        allowedPackagePermissionsTypes.put(permission, packageName);
        logger.debug("Granted permission '{}' to '{}.*'", permission, packageName);
    }

    /**
     * Registers a permission that modules are granted when working (directly or indirectly) through the given package
     *
     * @param packageName The package that requires the permission
     * @param permission The permission to grant
     */
    public void grantPermission(String packageName, Permission permission) {
        Preconditions.checkNotNull(packageName);
        Preconditions.checkNotNull(permission);
        if (System.getSecurityManager() != null) {
            System.getSecurityManager().checkPermission(UPDATE_ALLOWED_PERMISSIONS);
        }
        allowedPackagePermissionInstances.put(permission, packageName);
        logger.debug("Granted permission '{}' to '{}.*'", permission, packageName);
    }

    /**
     * Grants full, unqualified permission to a given package.
     * @param packageName The package to give full permission to
     */
    public void grantFullPermission(String packageName) {
        Preconditions.checkNotNull(packageName);
        if (System.getSecurityManager() != null) {
            System.getSecurityManager().checkPermission(UPDATE_ALLOWED_PERMISSIONS);
        }
        fullPrivilegePackages.add(packageName);
        logger.debug("Granted full permission to '{}.*'", packageName);
    }

    /**
     * Adds a class to the API - this allows it to be used directly from a module context.
     * @param clazz The class to add to the API.
     */
    public void addAPIClass(Class clazz) {
        Preconditions.checkNotNull(clazz);
        if (System.getSecurityManager() != null) {
            System.getSecurityManager().checkPermission(UPDATE_API_CLASSES);
        }
        apiClasses.add(clazz);
        logger.debug("Added API class '{}'", clazz);
    }

    /**
     * Adds a package to the API - this allows any class from the package to be used directly from a module context.
     * @param packageName The package to add to the API
     */
    public void addAPIPackage(String packageName) {
        Preconditions.checkNotNull(packageName);
        if (System.getSecurityManager() != null) {
            System.getSecurityManager().checkPermission(UPDATE_ALLOWED_PERMISSIONS);
        }
        apiPackages.add(packageName);
        logger.debug("Added API classes '{}.*'", packageName);
    }

    /**
     * Removes a permission that has previously been globally allowed
     *
     * @param permission The permission to revoke
     * @return Whether the permission was previously globally allowed
     */
    public boolean revokePermission(Class<? extends Permission> permission) {
        Preconditions.checkNotNull(permission);
        if (System.getSecurityManager() != null) {
            System.getSecurityManager().checkPermission(UPDATE_ALLOWED_PERMISSIONS);
        }
        logger.debug("Revoking global permission '{}'", permission);
        return globallyAllowedPermissionsTypes.remove(permission);
    }

    /**
     * Removes a permission that has previously been globally allowed
     *
     * @param permission The permission to revoke
     * @return Whether the permission was previously globally allowed
     */
    public boolean revokePermission(Permission permission) {
        Preconditions.checkNotNull(permission);
        if (System.getSecurityManager() != null) {
            System.getSecurityManager().checkPermission(UPDATE_ALLOWED_PERMISSIONS);
        }
        logger.debug("Revoking global permission '{}'", permission);
        return globallyAllowedPermissionsInstances.remove(permission);
    }

    /**
     * Remove a permission that has previously been granted to calls passing through a given class
     * WARNING: Does not revoke permissions granted at a package level
     *
     * @param apiType The api class to revoke the permission from
     * @param permission The permission to revoke
     * @return whether the permission had previously been granted to the given class.
     */
    public boolean revokePermission(Class apiType, Class<? extends Permission> permission) {
        Preconditions.checkNotNull(apiType);
        Preconditions.checkNotNull(permission);
        if (System.getSecurityManager() != null) {
            System.getSecurityManager().checkPermission(UPDATE_ALLOWED_PERMISSIONS);
        }
        logger.debug("Revoking permission '{}' from '{}'", permission, apiType);
        return allowedPermissionsTypes.remove(permission, apiType);
    }

    /**
     * Remove a permission that has previously been granted to calls passing through a given class
     * WARNING: Does not revoke permissions granted at a package level
     *
     * @param apiType The api class to revoke the permission from
     * @param permission The permission to revoke
     * @return whether the permission had previously been granted to the given class.
     */
    public boolean revokePermission(Class apiType, Permission permission) {
        Preconditions.checkNotNull(apiType);
        Preconditions.checkNotNull(permission);
        if (System.getSecurityManager() != null) {
            System.getSecurityManager().checkPermission(UPDATE_ALLOWED_PERMISSIONS);
        }
        logger.debug("Revoking permission '{}' from '{}'", permission, apiType);
        return allowedPermissionInstances.remove(permission, apiType);
    }

    /**
     * Remove a permission that has previously been granted to a calls passing through a given package.
     * This will also revoke permissions granted at a class level, for classes within the package
     *
     * @param packageName The package to revoke the permission from
     * @param permission The class of permission to revoke
     */
    public void revokePermission(String packageName, Class<? extends Permission> permission) {
        Preconditions.checkNotNull(packageName);
        Preconditions.checkNotNull(permission);
        if (System.getSecurityManager() != null) {
            System.getSecurityManager().checkPermission(UPDATE_ALLOWED_PERMISSIONS);
        }

        logger.debug("Revoking permission '{}' from '{}.*'", permission, packageName);
        allowedPackagePermissionsTypes.remove(permission, packageName);
        Iterator<Class> iterator = allowedPermissionsTypes.get(permission).iterator();
        while (iterator.hasNext()) {
            Class clazz = iterator.next();
            if (packageName.equals(clazz.getPackage().getName())) {
                iterator.remove();
            }
        }

    }

    /**
     * Remove a permission that has previously been granted to a calls passing through a given package.
     * This will also revoke permissions granted at a class level, for classes within the package
     *
     * @param packageName The package to revoke the permission from
     * @param permission The permission to revoke
     */
    public void revokePermission(String packageName, Permission permission) {
        Preconditions.checkNotNull(packageName);
        Preconditions.checkNotNull(permission);
        if (System.getSecurityManager() != null) {
            System.getSecurityManager().checkPermission(UPDATE_ALLOWED_PERMISSIONS);
        }

        logger.debug("Revoking permission '{}' from '{}.*'", permission, packageName);
        allowedPackagePermissionInstances.remove(permission, packageName);
        Iterator<Class> iterator = allowedPermissionInstances.get(permission).iterator();
        while (iterator.hasNext()) {
            Class clazz = iterator.next();
            if (packageName.equals(clazz.getPackage().getName())) {
                iterator.remove();
            }
        }
    }

    /**
     * Removes unqualified permission for the given package
     * @param packageName The name of the package
     * @return Whether full permission was originally granted to the granted to the package
     */
    public boolean revokeFullPermission(String packageName) {
        Preconditions.checkNotNull(packageName);
        if (System.getSecurityManager() != null) {
            System.getSecurityManager().checkPermission(UPDATE_ALLOWED_PERMISSIONS);
        }
        logger.debug("Revoking full permission from '{}.*'", packageName);
        return fullPrivilegePackages.remove(packageName);
    }

    /**
     * Removes a specific class from the list of API classes.
     * WARNING: This does not revoke access if granted at the package level.
     * @param clazz The class to remove from the API
     * @return Whether the class was perviously an API class
     */
    public boolean revokeAPIClass(Class clazz) {
        Preconditions.checkNotNull(clazz);
        if (System.getSecurityManager() != null) {
            System.getSecurityManager().checkPermission(UPDATE_API_CLASSES);
        }
        logger.debug("Removing from API '{}'", clazz);
        return apiClasses.remove(clazz);
    }

    /**
     * Removes a package and all contained classes from the list of API classes and packages.
     * @param packageName The package to remove from the API
     */
    public void revokeAPIPackage(String packageName) {
        Preconditions.checkNotNull(packageName);
        if (System.getSecurityManager() != null) {
            System.getSecurityManager().checkPermission(UPDATE_ALLOWED_PERMISSIONS);
        }

        logger.debug("Removing from API '{}.*'", packageName);
        apiPackages.remove(packageName);
        Iterator<Class> iterator = apiClasses.iterator();
        while (iterator.hasNext()) {
            Class clazz = iterator.next();
            if (packageName.equals(clazz.getPackage().getName())) {
                iterator.remove();
            }
        }
    }

    /**
     * @param type The class to check
     * @return Whether the given class is available to modules
     */
    @Override
    public boolean isAPIClass(Class type) {
        return apiClasses.contains(type) || apiPackages.contains(type.getPackage().getName());
    }

    @Override
    public void checkPermission(Permission perm) {
        if (!checkModAccess(perm)) {
            super.checkPermission(perm);
        }
    }

    @Override
    public void checkPermission(Permission perm, Object context) {
        if (!checkModAccess(perm)) {
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
    private boolean checkModAccess(Permission perm) {

        if (calculatingPermission.get() != null) {
            return true;
        }

        if (globallyAllowedPermissionsTypes.contains(perm.getClass()) || globallyAllowedPermissionsInstances.contains(perm)) {
            return true;
        }

        calculatingPermission.set(true);

        try {
            Class[] stack = getClassContext();
            for (int i = 0; i < stack.length; ++i) {
                ClassLoader owningLoader = stack[i].getClassLoader();
                if (owningLoader != null && owningLoader instanceof ModuleClassLoader) {
                    return checkAPIPermissionsFor(perm, i, stack);
                }
            }
        } finally {
            calculatingPermission.set(null);
        }
        return false;
    }

    /**
     * Checks the stack down to the first module to see if the given permission is allowed to be triggered from a module context.
     * @param permission The permission being checked
     * @param moduleDepth The depth of the first module class
     * @param stack The classes involved in the current stack
     * @return Whether the permission has been granted to any of the API classes involved.
     */
    private boolean checkAPIPermissionsFor(Permission permission, int moduleDepth, Class[] stack) {
        Set<Class> allowed = Sets.union(allowedPermissionInstances.get(permission), allowedPermissionsTypes.get(permission.getClass()));
        Set<String> allowedPackages = Sets.union(Sets.union(allowedPackagePermissionInstances.get(permission), allowedPackagePermissionsTypes.get(permission.getClass())),
                fullPrivilegePackages);
        for (int i = moduleDepth - 1; i >= 0; i--) {
            if (allowed.contains(stack[i]) || allowedPackages.contains(stack[i].getPackage().getName())) {
                return true;
            }
        }
        return false;
    }

}
