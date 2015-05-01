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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.module.Module;

import java.security.Permission;

/**
 * This provider factory wraps another factory. Whenever the other factory would deny access, this factory logs an error and grants permission.
 * <p>This is intended to allow code being developed to run regardless of permission issues, so that required permissions can be gathered.</p>
 * @author Immortius
 */
public class WarnOnlyProviderFactory implements PermissionProviderFactory {

    private static final Logger logger = LoggerFactory.getLogger(WarnOnlyProviderFactory.class);
    private PermissionProviderFactory wrappedFactory;

    /**
     * @param wrappedFactory Another permission factory to wrap.
     */
    public WarnOnlyProviderFactory(PermissionProviderFactory wrappedFactory) {
        this.wrappedFactory = wrappedFactory;
    }

    @Override
    public PermissionProvider createPermissionProviderFor(Module module) {
        return new PermissionProvider() {

            private PermissionProvider wrapped = wrappedFactory.createPermissionProviderFor(module);

            @Override
            public boolean isPermitted(Class<?> type) {
                if (!wrapped.isPermitted(type)) {
                    logger.error("Use of non-permitted class '{}' detected by module '{}': this should be fixed for production use", type.toString(), module);
                }
                return true;
            }

            @Override
            public boolean isPermitted(Permission permission, Class<?> context) {
                if (!wrapped.isPermitted(permission, context)) {
                    logger.error("Non-permitted permission '{}' required by module '{}', class '{}': this should be fixed for production use", permission, module, context);
                }
                return true;
            }
        };
    }
}
