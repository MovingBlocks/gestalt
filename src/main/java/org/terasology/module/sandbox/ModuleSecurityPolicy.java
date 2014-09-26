/*
 * Copyright 2014 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.module.sandbox;

import java.security.Permission;
import java.security.Policy;
import java.security.ProtectionDomain;

/**
 * This security policy enforces permissions but only for module code
 *
 * @author Immortius
 */
public class ModuleSecurityPolicy extends Policy {

    @Override
    public boolean implies(ProtectionDomain domain, Permission permission) {
        return !(domain.getClassLoader() instanceof ModuleClassLoader) || super.implies(domain, permission);
    }

    /**
     * Returns a modifiable Permissions collection, which is not used again, so JVisualVM can connect via RMI.
     */
    @Override
    public PermissionCollection getPermissions(CodeSource codesource) {
        return new Permissions();
    }

    /**
     * Returns a modifiable Permissions collection, which is not used again, so JVisualVM can connect via RMI.
     */
    @Override
    public PermissionCollection getPermissions(ProtectionDomain domain) {
        return new Permissions();
    }

}
