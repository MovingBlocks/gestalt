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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.module.resources.ModuleFileSource;
import org.terasology.naming.Name;
import org.terasology.naming.Version;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * A module is an identified and versioned set of code and/or resources that can be loaded and used at runtime. This class encapsulates information on a
 * module.
 *
 * @author Immortius
 */
public final class Module {

    private static final Logger logger = LoggerFactory.getLogger(Module.class);

    private final ModuleMetadata metadata;
    private final ImmutableList<ModuleFileSource> moduleFileSources;
    private final ImmutableList<URL> moduleClasspaths;
    private final Predicate<Class<?>> classPredicate;

    private final Reflections moduleManifest;

    /**
     * Creates a module composed of the given paths, classpaths, and described by the given metadata.
     *
     * @param metadata The metadata describing the module
     * @param fileSources Any sources of files that compose the module
     * @param classpaths Any extra classpaths to load for the module
     * @param moduleManifest A manifest of the contents of the module. This should indicate all classes and any classpath provided resources.
     *                       Additionally this provides additional information on classes such as what they inherit and what annotations they are flagged with.
     * @param classPredicate Predicate to determine what classes to include from the main classpath (classes from the unloaded classpaths will be included automatically)
     */
    public Module(ModuleMetadata metadata, Collection<ModuleFileSource> fileSources, Collection<URL> classpaths, Reflections moduleManifest, Predicate<Class<?>> classPredicate) {
        this.metadata = metadata;
        this.moduleFileSources = ImmutableList.copyOf(fileSources);
        this.moduleClasspaths = ImmutableList.copyOf(classpaths);
        this.classPredicate = classPredicate;
        this.moduleManifest = moduleManifest;
    }

    /**
     * @return A list of file sources composing the module
     */
    public ImmutableList<ModuleFileSource> getFileSources() {
        return moduleFileSources;
    }

    /**
     * @return A list of additional classpaths to load
     */
    public List<URL> getClasspaths() {
        return moduleClasspaths;
    }

    /**
     * @return The identifier for the module
     */
    public Name getId() {
        return metadata.getId();
    }

    /**
     * @return The version of the module
     */
    public Version getVersion() {
        return metadata.getVersion();
    }

    /**
     * @return The list of permission sets required by this module
     */
    public ImmutableSet<String> getRequiredPermissions() {
        return ImmutableSet.copyOf(metadata.getRequiredPermissions());
    }

    /**
     * @return Metadata describing the module
     */
    public ModuleMetadata getMetadata() {
        return metadata;
    }

    /**
     * @return Information on the contents on this module
     */
    public Reflections getModuleManifest() {
        return moduleManifest;
    }

    public Predicate<Class<?>> getClassPredicate() {
        return classPredicate;
    }

    //
//    /**
//     * Provides the partial reflection information for this module, in isolation of other modules.  This information is of limited use by itself - without combining
//     * it with the information from its dependencies, it will be unable to resolve subtypes if an intermediate class is missing. Discovered classes will also not be
//     * instantiable.
//     * <p>
//     * Intended for use in building a reflection information for a complete environment.
//     * </p>
//     *
//     * @return The partial reflection information for this module in isolation
//     */
//    public Reflections getReflectionsFragment() {
//        Preconditions.checkState(isCodeModule(), "Cannot get reflections fragment for non-code module");
//        if (reflectionsFragment == null) {
//            scanForReflections();
//        }
//        return reflectionsFragment;
//    }
//
//    private void scanForReflections() {
//        reflectionsFragment = new Reflections(new ConfigurationBuilder()
//                .addUrls(getClasspaths())
//                .setScanners(new TypeAnnotationsScanner(), new SubTypesScanner())
//                .addClassLoader(ClasspathHelper.staticClassLoader()));
//    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Module) {
            Module other = (Module) obj;
            return Objects.equals(other.getId(), getId()) && Objects.equals(other.getVersion(), getVersion());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getVersion());
    }

    @Override
    public String toString() {
        return getId() + "-" + getVersion();
    }

}
