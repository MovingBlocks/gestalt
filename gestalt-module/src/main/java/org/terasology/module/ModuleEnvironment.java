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

package org.terasology.module;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.module.filesystem.ModuleFileSystemProvider;
import org.terasology.module.sandbox.BytecodeInjector;
import org.terasology.module.sandbox.ModuleClassLoader;
import org.terasology.module.sandbox.ObtainClassloader;
import org.terasology.module.sandbox.PermissionProviderFactory;
import org.terasology.naming.Name;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.security.AccessControlException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * An environment composed of a set of modules. A chain of class loaders is created for each module that isn't on the classpath, such that dependencies appear before
 * dependants. Classes of interest can then be discovered by the types they inherit or annotations they have.
 * <p>
 * When the environment is no longer in use it should be closed - this closes all the class loaders. Memory used by the ClassLoaders will then be available for garbage
 * collection once the last instance of a class loaded from it is freed.
 * </p>
 *
 * @author Immortius
 */
public class ModuleEnvironment implements AutoCloseable, Iterable<Module> {

    private static final Logger logger = LoggerFactory.getLogger(ModuleEnvironment.class);

    private final ImmutableMap<Name, Module> modules;
    private final ClassLoader apiClassLoader;
    private final ClassLoader finalClassLoader;
    private final ImmutableList<ModuleClassLoader> managedClassLoaders;
    private final ImmutableSetMultimap<Name, Name> moduleDependencies;
    private final Reflections fullReflections;
    private final ImmutableList<Module> modulesOrderByDependencies;
    private final ImmutableList<Name> moduleIdsOrderedByDependencies;
    private final FileSystem fileSystem;


    /**
     * @param modules                   The modules this environment should encompass.
     * @param permissionProviderFactory A factory for producing a PermissionProvider for each loaded module
     * @throws java.lang.IllegalArgumentException if the Iterable contains multiple modules with the same id.
     */
    public ModuleEnvironment(Iterable<Module> modules, PermissionProviderFactory permissionProviderFactory) {
        this(modules, permissionProviderFactory, Collections.<BytecodeInjector>emptyList());
    }

    /**
     * @param modules                   The modules this environment should encompass.
     * @param permissionProviderFactory A factory for producing a PermissionProvider for each loaded module
     * @param injectors                 Any Bytecode Injectors that should be run over any loaded module class.
     * @throws java.lang.IllegalArgumentException if the Iterable contains multiple modules with the same id.
     */
    public ModuleEnvironment(Iterable<Module> modules, PermissionProviderFactory permissionProviderFactory, Iterable<BytecodeInjector> injectors) {
        this(modules, permissionProviderFactory, injectors, ModuleEnvironment.class.getClassLoader());
    }

    /**
     * @param modules                   The modules this environment should encompass.
     * @param permissionProviderFactory A factory for producing a PermissionProvider for each loaded module
     * @param injectors                 Any Bytecode Injectors that should be run over any loaded module class.
     * @param apiClassLoader            The base classloader the module environment should build upon.
     * @throws java.lang.IllegalArgumentException if the Iterable contains multiple modules with the same id.
     */
    public ModuleEnvironment(Iterable<Module> modules, final PermissionProviderFactory permissionProviderFactory,
                             final Iterable<BytecodeInjector> injectors, ClassLoader apiClassLoader) {
        Map<Name, Reflections> reflectionsByModule = Maps.newLinkedHashMap();
        this.modules = buildModuleMap(modules);
        this.apiClassLoader = apiClassLoader;
        this.modulesOrderByDependencies = calculateModulesOrderedByDependencies();
        this.moduleIdsOrderedByDependencies = ImmutableList.copyOf(Collections2.transform(modulesOrderByDependencies, new Function<Module, Name>() {

            @Override
            public Name apply(Module input) {
                return input.getId();
            }
        }));

        ImmutableList.Builder<ModuleClassLoader> managedClassLoaderListBuilder = ImmutableList.builder();
        ClassLoader lastClassLoader = apiClassLoader;
        List<Module> orderedModules = getModulesOrderedByDependencies();
        for (final Module module : orderedModules) {
            if (module.isCodeModule()) {
                Optional<ModuleClassLoader> classLoader = determineClassloaderFor(module, lastClassLoader, permissionProviderFactory, injectors);
                if (classLoader.isPresent()) {
                    managedClassLoaderListBuilder.add(classLoader.get());
                    lastClassLoader = classLoader.get();
                }
                reflectionsByModule.put(module.getId(), module.getReflectionsFragment());
            }
        }
        this.finalClassLoader = lastClassLoader;
        this.fullReflections = buildFullReflections(reflectionsByModule);
        this.managedClassLoaders = managedClassLoaderListBuilder.build();
        this.moduleDependencies = buildModuleDependencies();

        this.fileSystem = new ModuleFileSystemProvider().newFileSystem(this);

    }

    /**
     * Builds a map of modules, keyed by id, from an iterable.
     *
     * @param moduleList The list of modules to map
     * @return The final map
     */
    private ImmutableMap<Name, Module> buildModuleMap(Iterable<Module> moduleList) {
        ImmutableMap.Builder<Name, Module> builder = ImmutableMap.builder();
        for (Module module : moduleList) {
            builder.put(module.getId(), module);
        }
        return builder.build();
    }

    /**
     * @param module                    The module to determine the classloader for
     * @param parent                    The classloader to parent any new classloader off of
     * @param permissionProviderFactory The provider of api information
     * @param injectors                 Any Bytecode Injectors that should be run over any loaded module class.
     * @return The new module classloader to use for this module, or absent if the parent classloader should be used.
     */
    private Optional<ModuleClassLoader> determineClassloaderFor(final Module module, final ClassLoader parent,
                                                                final PermissionProviderFactory permissionProviderFactory, final Iterable<BytecodeInjector> injectors) {
        if (!module.isOnClasspath()) {
            ModuleClassLoader moduleClassloader = AccessController.doPrivileged(new PrivilegedAction<ModuleClassLoader>() {
                @Override
                public ModuleClassLoader run() {
                    return new ModuleClassLoader(module.getId(), module.getClasspaths().toArray(new URL[module.getClasspaths().size()]),
                            parent, permissionProviderFactory.createPermissionProviderFor(module), injectors);
                }
            });
            return Optional.of(moduleClassloader);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Builds Reflections information over the entire module environment, combining the information of all individual modules
     *
     * @param reflectionsByModule A map of reflection information for each module
     */
    private Reflections buildFullReflections(Map<Name, Reflections> reflectionsByModule) {
        ConfigurationBuilder fullBuilder = new ConfigurationBuilder()
                .addClassLoader(apiClassLoader)
                .addClassLoader(finalClassLoader);
        Reflections reflections = new Reflections(fullBuilder);
        for (Reflections moduleReflection : reflectionsByModule.values()) {
            reflections.merge(moduleReflection);
        }
        return reflections;
    }

    private ImmutableSetMultimap<Name, Name> buildModuleDependencies() {
        SetMultimap<Name, Name> moduleDependenciesBuilder = HashMultimap.create();
        for (Module module : getModulesOrderedByDependencies()) {
            for (DependencyInfo dependency : module.getMetadata().getDependencies()) {
                moduleDependenciesBuilder.put(module.getId(), dependency.getId());
                moduleDependenciesBuilder.putAll(module.getId(), moduleDependenciesBuilder.get(dependency.getId()));
            }
        }
        return ImmutableSetMultimap.copyOf(moduleDependenciesBuilder);
    }

    private ImmutableList<Module> calculateModulesOrderedByDependencies() {
        List<Module> result = Lists.newArrayList();
        List<Module> alphabeticallyOrderedModules = Lists.newArrayList(modules.values());
        Collections.sort(alphabeticallyOrderedModules, new Comparator<Module>() {
            @Override
            public int compare(Module o1, Module o2) {
                return o1.getId().compareTo(o2.getId());
            }
        });

        for (Module module : alphabeticallyOrderedModules) {
            addModuleAfterDependencies(module, result);
        }
        return ImmutableList.copyOf(result);
    }

    private void addModuleAfterDependencies(Module module, List<Module> out) {
        if (!out.contains(module)) {
            List<Name> dependencies = Lists.newArrayList(Collections2.transform(module.getMetadata().getDependencies(), new Function<DependencyInfo, Name>() {
                @Nullable
                @Override
                public Name apply(@Nullable DependencyInfo input) {
                    if (input != null) {
                        return input.getId();
                    }
                    return null;
                }
            }));
            Collections.sort(dependencies);
            for (Name dependency : dependencies) {
                Module dependencyModule = modules.get(dependency);
                if (dependencyModule != null) {
                    addModuleAfterDependencies(dependencyModule, out);
                }
            }
            out.add(module);
        }
    }

    @Override
    public void close() {
        for (ModuleClassLoader classLoader : managedClassLoaders) {
            try {
                classLoader.close();
            } catch (IOException e) {
                logger.error("Failed to close classLoader for module '" + classLoader.getModuleId() + "'", e);
            }
        }
    }

    /**
     * @param id The id of the module to return
     * @return The desired module, or null if it is not part of the environment
     */
    public Module get(Name id) {
        return modules.get(id);
    }

    /**
     * The resulting list is sorted so that dependencies appear before modules that depend on them. Additionally,
     * modules are alphabetically ordered where there are no dependencies.
     *
     * @return A list of modules in the environment, sorted so any dependencies appear before a module
     */
    public final List<Module> getModulesOrderedByDependencies() {
        return modulesOrderByDependencies;
    }

    /**
     * @return A list of modules in the environment, sorted so any dependencies appear before a module
     */
    public final List<Name> getModuleIdsOrderedByDependencies() {
        return moduleIdsOrderedByDependencies;
    }

    /**
     * Determines the module from which the give class originates from.
     *
     * @param type The type to find the module for
     * @return The module providing the class, or null if it doesn't come from a module.
     */
    public Name getModuleProviding(Class<?> type) {
        ClassLoader classLoader = AccessController.doPrivileged(new ObtainClassloader(type));
        if (classLoader instanceof ModuleClassLoader) {
            return ((ModuleClassLoader) classLoader).getModuleId();
        }
        try {
            Path sourceLocation = Paths.get(type.getProtectionDomain().getCodeSource().getLocation().toURI());
            for (Module module : modules.values()) {
                if (module.isCodeModule()) {
                    for (URL classpath : module.getClasspaths()) {
                        if (Paths.get(classpath.toURI()).equals(sourceLocation)) {
                            return module.getId();
                        }
                    }
                }
            }
        } catch (URISyntaxException e) {
            logger.error("Failed to convert url to uri for comparison", e);
        } catch (AccessControlException e) {
            logger.error("Cannot determinate Module for type: {}", type.getName());
        }
        return null;
    }

    /**
     * @param moduleId The id of the module to get the dependencies
     * @return The ids of the dependencies of the desired module
     */
    public Set<Name> getDependencyNamesOf(Name moduleId) {
        return moduleDependencies.get(moduleId);
    }

    /**
     * @param type The type to find subtypes of
     * @param <U>  The type to find subtypes of
     * @return A Iterable over all subtypes of type that appear in the module environment
     */
    public <U> Iterable<Class<? extends U>> getSubtypesOf(Class<U> type) {
        try {
            return fullReflections.getSubTypesOf(type);
        } catch (ReflectionsException e) {
            throw new ReflectionsException("Could not obtain subtypes of '" + type + "' - possible subclass without permission", e);
        }
    }

    /**
     * @param type   The type to find subtypes of
     * @param <U>    The type to find subtypes of
     * @param filter A filter to apply to the returned subtypes
     * @return A Iterable over all subtypes of type that appear in the module environment
     */
    public <U> Iterable<Class<? extends U>> getSubtypesOf(Class<U> type, Predicate<Class<?>> filter) {
        return Collections2.filter(fullReflections.getSubTypesOf(type), filter);
    }

    /**
     * @param annotation The annotation of interest
     * @return All types in the environment that are either marked by the given annotation, or are subtypes of a type marked with the annotation if the annotation is marked
     * as @Inherited
     */
    public Iterable<Class<?>> getTypesAnnotatedWith(Class<? extends Annotation> annotation) {
        return fullReflections.getTypesAnnotatedWith(annotation, true);
    }

    /**
     * @param annotation The annotation of interest
     * @param filter     Further filter on the returned types
     * @return All types in the environment that are either marked by the given annotation, or are subtypes of a type marked with the annotation if the annotation is marked
     * as @Inherited
     */
    public Iterable<Class<?>> getTypesAnnotatedWith(Class<? extends Annotation> annotation, Predicate<Class<?>> filter) {
        return Collections2.filter(fullReflections.getTypesAnnotatedWith(annotation, true), filter);
    }

    @Override
    public Iterator<Module> iterator() {
        return modules.values().iterator();
    }

    public FileSystem getFileSystem() {
        return fileSystem;
    }
}
