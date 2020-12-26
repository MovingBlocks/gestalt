package org.terasology.gestalt.di.scanners;

import org.terasology.context.BeanDefinition;
import org.terasology.gestalt.di.BeanEnvironment;
import org.terasology.gestalt.di.BeanScanner;
import org.terasology.gestalt.di.BeanUtilities;
import org.terasology.gestalt.di.ServiceRegistry;
import org.terasology.gestalt.di.injection.Qualifiers;

public class StandardScanner implements BeanScanner {
    private final String prefix;
    private final ClassLoader[] loaders;

    public StandardScanner(String prefix, ClassLoader... loaders) {
        this.prefix = prefix;
        this.loaders = loaders;
    }

    public StandardScanner(String prefix) {
        this.prefix = prefix;
        loaders = new ClassLoader[]{};
    }

    @Override
    public void apply(ServiceRegistry registry, BeanEnvironment environment) {
        if (loaders.length == 0) {
            for (ClassLoader loader : environment.classLoaders()) {
                for (BeanDefinition definition : environment.byPrefix(loader, prefix)) {
                    loadDefinition(definition, registry);
                }
            }
        } else {
            for (ClassLoader loader : this.loaders) {
                for (BeanDefinition definition : environment.byPrefix(loader, prefix)) {
                    loadDefinition(definition, registry);
                }
            }
        }
    }

    private void loadDefinition(BeanDefinition definition, ServiceRegistry registry) {
        registry.with(definition.targetClass())
            .lifetime(BeanUtilities.resolveLifetime(definition.getAnnotationMetadata()))
            .byQualifier(Qualifiers.resolveQualifier(definition.getAnnotationMetadata()));
    }
}
