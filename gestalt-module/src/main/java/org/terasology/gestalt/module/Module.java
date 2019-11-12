/*
 * Copyright 2019 MovingBlocks
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
package org.terasology.gestalt.module;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.gestalt.module.resources.ModuleFileSource;
import org.terasology.gestalt.naming.Name;
import org.terasology.gestalt.naming.Version;

import java.io.File;
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
    private final ModuleFileSource moduleFileSources;
    private final ImmutableList<File> moduleClasspaths;
    private final Predicate<Class<?>> classPredicate;

    private final Reflections moduleManifest;

    /**
     * Creates a module composed of the given paths, classpaths, and described by the given metadata.
     *
     * @param metadata       The metadata describing the module
     * @param fileSources    Any sources of files that compose the module. Must not be null - can be {@link org.terasology.gestalt.module.resources.EmptyFileSource}
     * @param classpaths     Any extra classpaths to load for the module
     * @param moduleManifest A manifest of the contents of the module. This should indicate all classes and any classpath provided resources.
     *                       Additionally this provides additional information on classes such as what they inherit and what annotations they are flagged with.
     * @param classPredicate Predicate to determine what classes to include from the main classpath (classes from the unloaded classpaths will be included automatically)
     */
    public Module(ModuleMetadata metadata, ModuleFileSource fileSources, Collection<File> classpaths, Reflections moduleManifest, Predicate<Class<?>> classPredicate) {
        Preconditions.checkNotNull(metadata);
        Preconditions.checkNotNull(fileSources);
        Preconditions.checkNotNull(moduleManifest);
        Preconditions.checkNotNull(classPredicate);
        this.metadata = metadata;
        this.moduleFileSources = fileSources;
        this.moduleClasspaths = ImmutableList.copyOf(classpaths);
        this.classPredicate = classPredicate;
        this.moduleManifest = moduleManifest;
        // Sometimes reflections loses the Resources store if it is empty? Debugging still, but this prevents it from breaking everything
        moduleManifest.getStore().getOrCreate("ResourcesScanner");
    }

    /**
     * @return A ModuleFileSource providing this module's resources
     */
    public ModuleFileSource getResources() {
        return moduleFileSources;
    }

    /**
     * @return A list of additional classpaths to load
     */
    public List<File> getClasspaths() {
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

    /**
     * @return A predicate that specifies whether a given class from the main classloader is a
     * member of this module
     */
    public Predicate<Class<?>> getClassPredicate() {
        return classPredicate;
    }

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
