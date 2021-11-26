package org.terasology.gestalt.module;

import org.terasology.gestalt.di.ServiceRegistry;
import org.terasology.gestalt.module.dependencyresolution.DependencyResolver;
import org.terasology.gestalt.module.sandbox.PermissionProviderFactory;

public class ModuleServiceRegistry extends ServiceRegistry {
    public ModuleServiceRegistry(PermissionProviderFactory permissionProviderFactory) {
        with(ModuleMetadataJsonAdapter.class);
        with(ModuleFactory.class);
        with(TableModuleRegistry.class);
        with(DependencyResolver.class);
        with(PermissionProviderFactory.class).use(() -> permissionProviderFactory);
    }
}
