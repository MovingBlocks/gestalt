package org.terasology.gestalt.module.di;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.module.TestImplementation1;
import org.terasology.context.Lifetime;
import org.terasology.gestalt.di.BeanContext;
import org.terasology.gestalt.di.DefaultBeanContext;
import org.terasology.gestalt.module.ModuleEnvironment;
import org.terasology.gestalt.module.ModulePathScanner;
import org.terasology.gestalt.module.ModuleRegistry;
import org.terasology.gestalt.module.ModuleServiceRegistry;
import org.terasology.gestalt.module.dependencyresolution.DependencyResolver;
import org.terasology.gestalt.module.sandbox.PermissionProviderFactory;
import org.terasology.gestalt.module.sandbox.PermitAllPermissionProviderFactory;
import org.terasology.gestalt.naming.Name;

import java.nio.file.Paths;
import java.util.List;

public class BeanContextInModuleTest {

    private ModuleEnvironment environment;

    @BeforeEach
    public void setup() {
        ModuleServiceRegistry serviceRegistry = new ModuleServiceRegistry(new PermitAllPermissionProviderFactory());
        serviceRegistry.with(ModulePathScanner.class).lifetime(Lifetime.Singleton);
        BeanContext root = new DefaultBeanContext(serviceRegistry);

        ModulePathScanner scanner = root.getBean(ModulePathScanner.class);
        ModuleRegistry registry = root.getBean(ModuleRegistry.class);
        scanner.scan(registry, Paths.get("test-modules").toFile());

        environment = new ModuleEnvironment(root,
                root.getBean(DependencyResolver.class).resolve(new Name("moduleB")).getModules(),
                root.getBean(PermissionProviderFactory.class));
    }

    @Test
    public void findByInterface() {
        List<? extends TestImplementation1> list = environment.getBeans(TestImplementation1.class);
        Assertions.assertFalse(list.isEmpty());
        Assertions.assertArrayEquals(
                new String[]{"org.module.b.DepByInterface"},
                list.stream().map(o -> o.getClass().getName()).toArray(String[]::new)
        );
    }
}
