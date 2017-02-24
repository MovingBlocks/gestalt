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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.naming.Name;
import org.terasology.naming.Version;

import javax.annotation.Nullable;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;

/**
 * A module is an identified and versioned set of code and/or resources that can be loaded and used at runtime. This class encapsulates information on a
 * module.
 *
 * @author Immortius
 */
public final class Module {

    private static final Logger logger = LoggerFactory.getLogger(Module.class);

    private final ImmutableList<Path> paths;
    private final ImmutableList<URL> classpaths;
    private final ModuleMetadata metadata;
    private final boolean classesAlreadyLoaded;

    private Reflections reflectionsFragment;

    /**
     * Creates a module composed of the given paths, classpaths, and described by the given metadata.
     *
     * @param paths      The paths composing the module
     * @param classpaths The classpaths composing the module. These may be root directories for classes, or jar locations
     * @param metadata   The metadata describing the module
     * @param reflectionsFragment A Reflections instance for this module. This provides information on the available classes in the module. Can be null, in which case
     *                            a new reflections instance will be generated when required (containing subclass, annotation and resource information)
     */
    public Module(Collection<Path> paths, Collection<URL> classpaths, ModuleMetadata metadata, @Nullable Reflections reflectionsFragment) {
        this(paths, classpaths, false, metadata, reflectionsFragment);
    }

    /**
     * Creates a module composed of the given paths, classpaths, and described by the given metadata.
     *
     * @param paths                The paths composing the module
     * @param classpaths           The classpaths composing the module. These may be root directories for classes, or jar locations
     * @param classesAlreadyLoaded Are the classes on this module's classpaths already loaded. If so no extra classloader should be created for this module.
     * @param metadata             The metadata describing the module
     * @param reflectionsFragment A Reflections instance for this module. This provides information on the available classes in the module. Can be null, in which case
     *                            a new reflections instance will be generated when required (containing subclass, annotation and resource information)
     */
    public Module(Collection<Path> paths, Collection<URL> classpaths, boolean classesAlreadyLoaded, ModuleMetadata metadata, @Nullable Reflections reflectionsFragment) {
        this.paths = ImmutableList.copyOf(paths);
        this.metadata = metadata;
        this.classesAlreadyLoaded = classesAlreadyLoaded;
        this.classpaths = ImmutableList.copyOf(classpaths);
        this.reflectionsFragment = reflectionsFragment;
    }

    /**
     * @return The locations composing the module
     */
    public ImmutableList<Path> getLocations() {
        return paths;
    }

    /**
     * @return The urls forming the classpath of the module
     */
    public ImmutableList<URL> getClasspaths() {
        return classpaths;
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
     * Whether the module is included in the classpath of the application. These are not loaded dynamically and hence are not sandboxed and are always active.
     *
     * @return Whether this module is on the classpath
     */
    public boolean isOnClasspath() {
        return classesAlreadyLoaded;
    }

    /**
     * @return Whether the module may introduce code elements
     */
    public boolean isCodeModule() {
        return !classpaths.isEmpty();
    }

    /**
     * @return Metadata describing the module
     */
    public ModuleMetadata getMetadata() {
        return metadata;
    }

    /**
     * Provides the partial reflection information for this module, in isolation of other modules.  This information is of limited use by itself - without combining
     * it with the information from its dependencies, it will be unable to resolve subtypes if an intermediate class is missing. Discovered classes will also not be
     * instantiable.
     * <p>
     * Intended for use in building a reflection information for a complete environment.
     * </p>
     *
     * @return The partial reflection information for this module in isolation
     */
    public Reflections getReflectionsFragment() {
        Preconditions.checkState(isCodeModule(), "Cannot get reflections fragment for non-code module");
        if (reflectionsFragment == null) {
            scanForReflections();
        }
        return reflectionsFragment;
    }

    private void scanForReflections() {
        reflectionsFragment = new Reflections(new ConfigurationBuilder()
                .addUrls(getClasspaths())
                .setScanners(new TypeAnnotationsScanner(), new SubTypesScanner())
                .addClassLoader(ClasspathHelper.staticClassLoader()));
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
