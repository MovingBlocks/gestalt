/*
 * Copyright 2014 MovingBlocks
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

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.module.sandbox.BytecodeInjector;
import org.terasology.module.sandbox.ModuleClassLoader;
import org.terasology.module.sandbox.ObtainClassloader;
import org.terasology.module.sandbox.PermissionProviderFactory;
import org.terasology.naming.Name;
import org.terasology.util.collection.UniqueQueue;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An environment composed of a set of modules. A chain of class loaders is created for each module that isn't on the classpath, such that dependencies appear before
 * dependants. Classes of interest can then be discovered by the types they inherit or annotations they have.
 * <p/>
 * When the environment is no longer in use it should be closed - this closes all the class loaders. Memory used by the ClassLoaders will then be available for garbage
 * collection once the last instance of a class loaded from it is freed.
 *
 * @author Immortius
 */
public class ModuleEnvironment implements AutoCloseable, Iterable<Module> {

    private static final Logger logger = LoggerFactory.getLogger(ModuleEnvironment.class);

    private final Map<Name, Module> modules;
    private final ClassLoader apiClassLoader;
    private final ClassLoader finalClassLoader;
    private final List<ModuleClassLoader> managedClassLoaders = Lists.newArrayList();
    private Reflections fullReflections;

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

        ClassLoader lastClassloader = apiClassLoader;
        List<Module> orderedModules = getModulesOrderedByDependencies();
        for (final Module module : orderedModules) {
            if (module.isCodeModule()) {
                lastClassloader = determineClassloaderFor(module, lastClassloader, permissionProviderFactory, injectors);
                reflectionsByModule.put(module.getId(), module.getReflectionsFragment());
            }
        }
        this.finalClassLoader = lastClassloader;
        buildFullReflections(reflectionsByModule);
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
     * @return The classloader to use for module - may be a newly created classloader.
     */
    private ClassLoader determineClassloaderFor(final Module module, final ClassLoader parent,
                                                final PermissionProviderFactory permissionProviderFactory, final Iterable<BytecodeInjector> injectors) {
        if (!module.isOnClasspath()) {
            ModuleClassLoader moduleClassloader = AccessController.doPrivileged(new PrivilegedAction<ModuleClassLoader>() {
                @Override
                public ModuleClassLoader run() {
                    return new ModuleClassLoader(module.getId(), module.getClasspaths().toArray(new URL[module.getClasspaths().size()]),
                            parent, permissionProviderFactory.createPermissionProviderFor(module), injectors);
                }
            });
            managedClassLoaders.add(moduleClassloader);
            return moduleClassloader;
        } else {
            return parent;
        }
    }

    /**
     * Builds Reflections information over the entire module environment, combining the information of all individual modules
     *
     * @param reflectionsByModule A map of reflection information for each module
     */
    private void buildFullReflections(Map<Name, Reflections> reflectionsByModule) {
        ConfigurationBuilder fullBuilder = new ConfigurationBuilder()
                .addClassLoader(apiClassLoader)
                .addClassLoader(finalClassLoader);
        fullReflections = new Reflections(fullBuilder);
        for (Reflections moduleReflection : reflectionsByModule.values()) {
            fullReflections.merge(moduleReflection);
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
     * @return A list of modules in the environment, sorted so any dependencies appear before a module
     */
    public final List<Module> getModulesOrderedByDependencies() {
        List<Module> result = Lists.newArrayList();
        for (Module module : modules.values()) {
            addModuleAfterDependencies(module, result);
        }
        return result;
    }

    /**
     * @return A list of modules in the environment, sorted so any dependencies appear before a module
     */
    public final List<Name> getModuleIdsOrderedByDependencies() {
        List<Module> orderedModules = Lists.newArrayList();
        for (Module module : modules.values()) {
            addModuleAfterDependencies(module, orderedModules);
        }
        List<Name> result = Lists.newArrayListWithCapacity(orderedModules.size());
        for (Module module : orderedModules) {
            result.add(module.getId());
        }
        return result;
    }

    private void addModuleAfterDependencies(Module module, List<Module> out) {
        if (!out.contains(module)) {
            for (DependencyInfo dependency : module.getMetadata().getDependencies()) {
                Module dependencyModule = modules.get(dependency.getId());
                if (dependencyModule != null) {
                    addModuleAfterDependencies(dependencyModule, out);
                }
            }
            out.add(module);
        }
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
        URL sourceUrl = type.getProtectionDomain().getCodeSource().getLocation();
        for (Module module : modules.values()) {
            if (module.isCodeModule() && module.getClasspaths().contains(sourceUrl)) {
                return module.getId();
            }
        }
        return null;
    }

    /**
     * @param moduleId The id of the module to get the dependencies
     * @return The ids of the dependencies of the desired module
     */
    public Set<Name> getDependencyNamesOf(Name moduleId) {
        Set<Name> result = Sets.newLinkedHashSet();
        UniqueQueue<Name> toProcess = new UniqueQueue<>();
        toProcess.add(moduleId);
        while (!toProcess.isEmpty()) {
            Name id = toProcess.remove();
            Module module = get(id);
            for (DependencyInfo dependency : module.getMetadata().getDependencies()) {
                result.add(dependency.getId());
                toProcess.add(dependency.getId());
            }
        }
        return result;
    }

    /**
     * @param type The type to find subtypes of
     * @param <U>  The type to find subtypes of
     * @return A Iterable over all subtypes of type that appear in the module environment
     */
    public <U> Iterable<Class<? extends U>> getSubtypesOf(Class<U> type) {
        return fullReflections.getSubTypesOf(type);
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
     * @return All types in the environment that are either marked by the given annotation, or are subtypes of a type marked with the annotation
     */
    public Iterable<Class<?>> getTypesAnnotatedWith(Class<? extends Annotation> annotation) {
        return fullReflections.getTypesAnnotatedWith(annotation);
    }

    /**
     * @param annotation            The annotation of interest
     * @param includeViaInheritance Whether to include classes that inherit a class marked with the annotation
     * @return All types marked with the annotation, or subtypes of types marked with the annotation if includeViaInheritance is true
     */
    public Iterable<Class<?>> getTypesAnnotatedWith(Class<? extends Annotation> annotation, boolean includeViaInheritance) {
        return fullReflections.getTypesAnnotatedWith(annotation, includeViaInheritance);
    }

    /**
     * @param annotation The annotation of interest
     * @param filter     Further filter on the returned types
     * @return All types in the environment that are either marked by the given annotation, or are subtypes of a type marked with the annotation
     */
    public Iterable<Class<?>> getTypesAnnotatedWith(Class<? extends Annotation> annotation, Predicate<Class<?>> filter) {
        return Collections2.filter(fullReflections.getTypesAnnotatedWith(annotation), filter);
    }

    /**
     * @param annotation            The annotation of interest
     * @param includeViaInheritance Whether to include classes that inherit a class marked with the annotation
     * @param filter                Further filter on the returned types
     * @return All types marked with the annotation, or subtypes of types marked with the annotation if includeViaInheritance is true
     */
    public Iterable<Class<?>> getTypesAnnotatedWith(Class<? extends Annotation> annotation, boolean includeViaInheritance, Predicate<Class<?>> filter) {
        return Collections2.filter(fullReflections.getTypesAnnotatedWith(annotation, includeViaInheritance), filter);
    }

    @Override
    public Iterator<Module> iterator() {
        return Iterators.unmodifiableIterator(modules.values().iterator());
    }
}
