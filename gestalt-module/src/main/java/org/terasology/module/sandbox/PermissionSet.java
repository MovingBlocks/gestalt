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

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.reflect.Reflection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Permission;
import java.util.Iterator;
import java.util.Set;

/**
 * A permission set is a group of Permissions and class access that can be granted together to a module.
 *
 * @author Immortius
 */
public class PermissionSet {

    private static final Logger logger = LoggerFactory.getLogger(PermissionSet.class);

    private final Set<Class<?>> apiClasses = Sets.newHashSet();
    private final Set<String> apiPackages = Sets.newHashSet();
    private final Set<Class<? extends Permission>> globallyAllowedPermissionsTypes = Sets.newHashSet();
    private final Set<Permission> globallyAllowedPermissionsInstances = Sets.newHashSet();
    private final SetMultimap<Class<? extends Permission>, Class<?>> allowedPermissionsTypes = HashMultimap.create();
    private final SetMultimap<Permission, Class<?>> allowedPermissionInstances = HashMultimap.create();
    private final SetMultimap<Class<? extends Permission>, String> allowedPackagePermissionsTypes = HashMultimap.create();
    private final SetMultimap<Permission, String> allowedPackagePermissionInstances = HashMultimap.create();

    /**
     * @param type The type to check whether access is permitted to
     * @return Whether access to this type is granted by the permission set
     */
    public boolean isPermitted(Class<?> type) {
        return apiClasses.contains(type) || apiPackages.contains(Reflection.getPackageName(type));
    }

    /**
     * @param permission The permission to check
     * @param context The context to check
     * @return Whether the given permission is granted in the given context, by this permission set
     */
    public boolean isPermitted(Permission permission, Class<?> context) {
        return globallyAllowedPermissionsTypes.contains(permission.getClass()) || globallyAllowedPermissionsInstances.contains(permission)
                || allowedPermissionInstances.get(permission).contains(context)
                || allowedPermissionsTypes.get(permission.getClass()).contains(context)
                || allowedPackagePermissionInstances.get(permission).contains(Reflection.getPackageName(context))
                || allowedPackagePermissionsTypes.get(permission.getClass()).contains(Reflection.getPackageName(context));
    }

    /**
     * Registers a global permission that all modules are granted
     *
     * @param permission The class of permission to grant
     */
    public void grantPermission(Class<? extends Permission> permission) {
        Preconditions.checkNotNull(permission);
        if (System.getSecurityManager() != null) {
            System.getSecurityManager().checkPermission(ModuleSecurityManager.UPDATE_ALLOWED_PERMISSIONS);
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
            System.getSecurityManager().checkPermission(ModuleSecurityManager.UPDATE_ALLOWED_PERMISSIONS);
        }
        globallyAllowedPermissionsInstances.add(permission);
        logger.debug("Globally granted permission '{}'", permission);
    }

    /**
     * Registers a permission that modules are granted when working (directly or indirectly) through the given apiType
     *
     * @param apiType    The type that requires the permission
     * @param permission The class of permission to grant
     */
    public void grantPermission(Class<?> apiType, Class<? extends Permission> permission) {
        Preconditions.checkNotNull(apiType);
        Preconditions.checkNotNull(permission);
        if (System.getSecurityManager() != null) {
            System.getSecurityManager().checkPermission(ModuleSecurityManager.UPDATE_ALLOWED_PERMISSIONS);
        }
        allowedPermissionsTypes.put(permission, apiType);
        logger.debug("Granted permission '{}' to '{}'", permission, apiType);
    }

    /**
     * Registers a permission that modules are granted when working (directly or indirectly) through the given apiType
     *
     * @param apiType    The type that requires the permission
     * @param permission The permission to grant
     */
    public void grantPermission(Class<?> apiType, Permission permission) {
        Preconditions.checkNotNull(apiType);
        Preconditions.checkNotNull(permission);
        if (System.getSecurityManager() != null) {
            System.getSecurityManager().checkPermission(ModuleSecurityManager.UPDATE_ALLOWED_PERMISSIONS);
        }
        allowedPermissionInstances.put(permission, apiType);
        logger.debug("Granted permission '{}' to '{}'", permission, apiType);
    }

    /**
     * Registers a permission that modules are granted when working (directly or indirectly) through the given package
     *
     * @param packageName The package that requires the permission
     * @param permission  The class of permission to grant
     */
    public void grantPermission(String packageName, Class<? extends Permission> permission) {
        Preconditions.checkNotNull(packageName);
        Preconditions.checkNotNull(permission);
        if (System.getSecurityManager() != null) {
            System.getSecurityManager().checkPermission(ModuleSecurityManager.UPDATE_ALLOWED_PERMISSIONS);
        }
        allowedPackagePermissionsTypes.put(permission, packageName);
        logger.debug("Granted permission '{}' to '{}.*'", permission, packageName);
    }

    /**
     * Registers a permission that modules are granted when working (directly or indirectly) through the given package
     *
     * @param packageName The package that requires the permission
     * @param permission  The permission to grant
     */
    public void grantPermission(String packageName, Permission permission) {
        Preconditions.checkNotNull(packageName);
        Preconditions.checkNotNull(permission);
        if (System.getSecurityManager() != null) {
            System.getSecurityManager().checkPermission(ModuleSecurityManager.UPDATE_ALLOWED_PERMISSIONS);
        }
        allowedPackagePermissionInstances.put(permission, packageName);
        logger.debug("Granted permission '{}' to '{}.*'", permission, packageName);
    }

    /**
     * Adds a class to the API - this allows it to be used directly from a module context.
     *
     * @param clazz The class to add to the API.
     */
    public void addAPIClass(Class<?> clazz) {
        Preconditions.checkNotNull(clazz);
        if (System.getSecurityManager() != null) {
            System.getSecurityManager().checkPermission(ModuleSecurityManager.UPDATE_API_CLASSES);
        }
        apiClasses.add(clazz);
        logger.debug("Added API class '{}'", clazz);
    }

    /**
     * Adds a package to the API - this allows any class from the package to be used directly from a module context.
     *
     * @param packageName The package to add to the API
     */
    public void addAPIPackage(String packageName) {
        Preconditions.checkNotNull(packageName);
        if (System.getSecurityManager() != null) {
            System.getSecurityManager().checkPermission(ModuleSecurityManager.UPDATE_ALLOWED_PERMISSIONS);
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
            System.getSecurityManager().checkPermission(ModuleSecurityManager.UPDATE_ALLOWED_PERMISSIONS);
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
            System.getSecurityManager().checkPermission(ModuleSecurityManager.UPDATE_ALLOWED_PERMISSIONS);
        }
        logger.debug("Revoking global permission '{}'", permission);
        return globallyAllowedPermissionsInstances.remove(permission);
    }

    /**
     * Remove a permission that has previously been granted to calls passing through a given class
     * WARNING: Does not revoke permissions granted at a package level
     *
     * @param apiType    The api class to revoke the permission from
     * @param permission The permission to revoke
     * @return whether the permission had previously been granted to the given class.
     */
    public boolean revokePermission(Class<?> apiType, Class<? extends Permission> permission) {
        Preconditions.checkNotNull(apiType);
        Preconditions.checkNotNull(permission);
        if (System.getSecurityManager() != null) {
            System.getSecurityManager().checkPermission(ModuleSecurityManager.UPDATE_ALLOWED_PERMISSIONS);
        }
        logger.debug("Revoking permission '{}' from '{}'", permission, apiType);
        return allowedPermissionsTypes.remove(permission, apiType);
    }

    /**
     * Remove a permission that has previously been granted to calls passing through a given class
     * WARNING: Does not revoke permissions granted at a package level
     *
     * @param apiType    The api class to revoke the permission from
     * @param permission The permission to revoke
     * @return whether the permission had previously been granted to the given class.
     */
    public boolean revokePermission(Class<?> apiType, Permission permission) {
        Preconditions.checkNotNull(apiType);
        Preconditions.checkNotNull(permission);
        if (System.getSecurityManager() != null) {
            System.getSecurityManager().checkPermission(ModuleSecurityManager.UPDATE_ALLOWED_PERMISSIONS);
        }
        logger.debug("Revoking permission '{}' from '{}'", permission, apiType);
        return allowedPermissionInstances.remove(permission, apiType);
    }

    /**
     * Remove a permission that has previously been granted to a calls passing through a given package.
     * This will also revoke permissions granted at a class level, for classes within the package
     *
     * @param packageName The package to revoke the permission from
     * @param permission  The class of permission to revoke
     */
    public void revokePermission(String packageName, Class<? extends Permission> permission) {
        Preconditions.checkNotNull(packageName);
        Preconditions.checkNotNull(permission);
        if (System.getSecurityManager() != null) {
            System.getSecurityManager().checkPermission(ModuleSecurityManager.UPDATE_ALLOWED_PERMISSIONS);
        }

        logger.debug("Revoking permission '{}' from '{}.*'", permission, packageName);
        allowedPackagePermissionsTypes.remove(permission, packageName);
        Iterator<Class<?>> iterator = allowedPermissionsTypes.get(permission).iterator();
        while (iterator.hasNext()) {
            Class<?> clazz = iterator.next();
            if (packageName.equals(Reflection.getPackageName(clazz))) {
                iterator.remove();
            }
        }

    }

    /**
     * Remove a permission that has previously been granted to a calls passing through a given package.
     * This will also revoke permissions granted at a class level, for classes within the package
     *
     * @param packageName The package to revoke the permission from
     * @param permission  The permission to revoke
     */
    public void revokePermission(String packageName, Permission permission) {
        Preconditions.checkNotNull(packageName);
        Preconditions.checkNotNull(permission);
        if (System.getSecurityManager() != null) {
            System.getSecurityManager().checkPermission(ModuleSecurityManager.UPDATE_ALLOWED_PERMISSIONS);
        }

        logger.debug("Revoking permission '{}' from '{}.*'", permission, packageName);
        allowedPackagePermissionInstances.remove(permission, packageName);
        Iterator<Class<?>> iterator = allowedPermissionInstances.get(permission).iterator();
        while (iterator.hasNext()) {
            Class<?> clazz = iterator.next();
            if (packageName.equals(Reflection.getPackageName(clazz))) {
                iterator.remove();
            }
        }
    }

    /**
     * Removes a specific class from the list of API classes.
     * WARNING: This does not revoke access if granted at the package level.
     *
     * @param clazz The class to remove from the API
     * @return Whether the class was perviously an API class
     */
    public boolean revokeAPIClass(Class<?> clazz) {
        Preconditions.checkNotNull(clazz);
        if (System.getSecurityManager() != null) {
            System.getSecurityManager().checkPermission(ModuleSecurityManager.UPDATE_API_CLASSES);
        }
        logger.debug("Removing from API '{}'", clazz);
        return apiClasses.remove(clazz);
    }

    /**
     * Removes a package and all contained classes from the list of API classes and packages.
     *
     * @param packageName The package to remove from the API
     */
    public void revokeAPIPackage(String packageName) {
        Preconditions.checkNotNull(packageName);
        if (System.getSecurityManager() != null) {
            System.getSecurityManager().checkPermission(ModuleSecurityManager.UPDATE_ALLOWED_PERMISSIONS);
        }

        logger.debug("Removing from API '{}.*'", packageName);
        apiPackages.remove(packageName);
        Iterator<Class<?>> iterator = apiClasses.iterator();
        while (iterator.hasNext()) {
            Class<?> clazz = iterator.next();
            if (packageName.equals(Reflection.getPackageName(clazz))) {
                iterator.remove();
            }
        }
    }
}
