package org.terasology.gestalt.module;

import org.terasology.gestalt.di.ServiceRegistry;
import org.terasology.gestalt.module.dependencyresolution.DependencyResolver;
import org.terasology.gestalt.module.sandbox.PermissionProviderFactory;

public class ModuleServiceRegistry extends ServiceRegistry {
    public ModuleServiceRegistry(Class<? extends PermissionProviderFactory> permissionProviderFactory) {
        with(ModulePathScanner.class);
        with(ModuleMetadataJsonAdapter.class);
        with(ModulePathScanner.class);
        with(ModuleFactory.class);
        with(TableModuleRegistry.class);
        with(DependencyResolver.class);
        with(permissionProviderFactory);
    }
}
