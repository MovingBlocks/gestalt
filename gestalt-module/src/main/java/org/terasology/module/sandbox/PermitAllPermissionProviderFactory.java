package org.terasology.module.sandbox;

import org.terasology.module.Module;

import java.security.Permission;

public class PermitAllPermissionProviderFactory implements PermissionProviderFactory {

    @Override
    public PermissionProvider createPermissionProviderFor(Module module) {
        return new PermissionProvider() {
            @Override
            public boolean isPermitted(Class<?> type) {
                return true;
            }

            @Override
            public boolean isPermitted(Permission permission, Class<?> context) {
                return true;
            }
        };
    }
}
