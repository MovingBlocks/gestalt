package org.terasology.gestalt.di;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.module.TestImplementation1;
import org.terasology.context.BeanDefinition;
import org.terasology.gestalt.module.Module;
import org.terasology.gestalt.module.ModulePathScanner;
import org.terasology.gestalt.module.TableModuleRegistry;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class BeanEnvironmentTest {
    private BeanEnvironment environment ;
    private TableModuleRegistry registry;

    @Before
    public void setup() {
        environment = new BeanEnvironment();
        registry = new TableModuleRegistry();

        new ModulePathScanner().scan(registry, Paths.get("test-modules").toFile());
        for (Iterator<Module> it = registry.iterator(); it.hasNext(); ) {
            Module m = it.next();
            URL[] urls = m.getClasspaths().stream().map(x -> {
                try {
                    return x.toURI().toURL();
                } catch (MalformedURLException e) {
                    return null;
                }
            }).filter(Objects::nonNull).toArray(URL[]::new);
            environment.loadDefinitions(new URLClassLoader(urls));
        }
    }

    @Test
    public void lookupByInterface() {
        List<String> results = new ArrayList<>();
        for(BeanDefinition<?> it:  environment.byInterface(TestImplementation1.class)){
            results.add(it.targetClass().getName());
        }
        Assert.assertArrayEquals(Arrays.asList(
            "org.module.b.DepByInterface"
        ).toArray(),results.toArray());
    }

    @Test
    public void testAllDefinitionsByPrefix() {
        List<String> results = new ArrayList<>();
        for (BeanDefinition<?> it: environment.byPrefix("org.module.a")) {
            results.add(it.targetClass().getName());
        }
        results.sort(Comparator.naturalOrder());
        Assert.assertArrayEquals(Arrays.asList(
            "org.module.a.DepA",
            "org.module.a.DepB",
            "org.module.a.DepC"
        ).toArray(),results.toArray());

    }
}
