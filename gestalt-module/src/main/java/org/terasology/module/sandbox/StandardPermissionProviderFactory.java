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

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * A permission provider factory that gives each module permissions based on a number of permission sets, where each module has access to the
 * default set of permissions and can request additional permission sets.
 * @see org.terasology.module.sandbox.PermissionSet
 * @author Immortius
 */
public class StandardPermissionProviderFactory implements PermissionProviderFactory {

    public static final String BASE_PERMISSION_SET = "";
    private static final Logger logger = LoggerFactory.getLogger(StandardPermissionProviderFactory.class);
    private final Map<String, PermissionSet> permissionSets = Maps.newHashMap();

    public StandardPermissionProviderFactory() {
        permissionSets.put(BASE_PERMISSION_SET, new PermissionSet());
    }

    /**
     * @return The base permission set, which all modules are granted
     */
    public PermissionSet getBasePermissionSet() {
        return getPermissionSet(BASE_PERMISSION_SET);
    }

    /**
     * @param name The name of the permission set
     * @return The permission set with the given name, or null
     */
    public PermissionSet getPermissionSet(String name) {
        return permissionSets.get(name);
    }

    /**
     * Adds or replaces a permission set that modules can request
     * @param name The name to give the permission set
     * @param permissionSet A permission set to associate with the given name
     */
    public void addPermissionSet(String name, PermissionSet permissionSet) {
        this.permissionSets.put(name, permissionSet);
    }

    @Override
    public PermissionProvider createPermissionProviderFor(Module module, Predicate<Class<?>> isClasspathModuleClass) {
        List<PermissionProvider> grantedPermissionSets = Lists.newArrayList();
        grantedPermissionSets.add(getBasePermissionSet());
        for (String permissionSetId : module.getRequiredPermissions()) {
            PermissionSet set = getPermissionSet(permissionSetId);
            if (set != null) {
                grantedPermissionSets.add(set);
            } else {
                logger.warn("Module '{}' requires unknown permission '{}'", module, permissionSetId);
            }
        }
        grantedPermissionSets.add(new PredicatePermissionSet(isClasspathModuleClass));
        return new SetUnionPermissionProvider(grantedPermissionSets);
    }
}
