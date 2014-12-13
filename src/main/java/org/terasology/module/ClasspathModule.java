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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.module.exceptions.InvalidModulePathException;
import org.terasology.util.Varargs;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.Collection;
import java.util.Set;

/**
 * A module that exists on the classpath. This means that it is always loaded and available, so should not be loaded in a secondary class loader.
 *
 * @author Immortius
 */
public final class ClasspathModule extends BaseModule {

    private static final Logger logger = LoggerFactory.getLogger(ClasspathModule.class);
    private final ImmutableList<URL> classpaths;

    /**
     * @param metadata Module metadata describing this module
     * @param paths    a collection of paths to the module locations - may be a mixture of file and directory paths
     */
    private ClasspathModule(ModuleMetadata metadata, Collection<Path> paths) {
        super(paths, metadata);
        logger.debug("Creating ClasspathModule '{}' with paths '{}'", metadata.getId(), paths);
        ImmutableList.Builder<URL> builder = ImmutableList.builder();
        for (Path path : paths) {
            try {
                builder.add(path.toUri().toURL());
            } catch (MalformedURLException e) {
                throw new InvalidModulePathException("Path cannot be converted to URL: " + path, e);
            }
        }
        classpaths = builder.build();
    }

    /**
     * Creates a classpath module from a set of code sources.
     * <p/>
     * There is an option to include directories on the classpath. This should only be done for one classpath module - this is for use when running from source
     * in environments that keep resources and classes in separate locations (e.g. gradle by default). Any directory on the classpath (as opposed to jars) will be
     * included in this module
     *
     * @param metadata           Metadata describing the module to create
     * @param includeDirectories Whether to include directories on the classpath.
     * @param primarySource      The first source to include in this module
     * @param additionalSources  Any additional sources to include
     * @return A new ClasspathModule
     * @throws URISyntaxException If a source location cannot be converted to a proper URI (typically because the path to the source includes an invalid character).
     */
    public static ClasspathModule create(ModuleMetadata metadata, boolean includeDirectories, CodeSource primarySource, CodeSource... additionalSources)
            throws URISyntaxException {
        Set<Path> paths = Sets.newLinkedHashSet();

        for (CodeSource source : Varargs.combineToSet(primarySource, additionalSources)) {
            paths.add(Paths.get(source.getLocation().toURI()));
        }
        if (includeDirectories) {
            addClasspathDirectories(paths);
        }
        return new ClasspathModule(metadata, paths);
    }

    /**
     * Creates a classpath module from a set of representative classes. The code source (e.g. Jar or directory) for each class is included in the Classpath module.
     *
     * @param metadata          Metadata describing the module to create
     * @param primaryClass      The first representative class to include in the module
     * @param additionalClasses Any additional representative classes to include.
     * @return A new ClasspathModule
     * @throws URISyntaxException If a source location cannot be converted to a proper URI (typically because the path to the source includes an invalid character).
     */
    public static ClasspathModule create(ModuleMetadata metadata, Class<?> primaryClass, Class<?>... additionalClasses) throws URISyntaxException {
        return create(metadata, false, primaryClass, additionalClasses);
    }

    /**
     * Creates a classpath module from a set of representative classes. The code source (e.g. Jar or directory) for each class is included in the Classpath module.
     * <p/>
     * There is an option to include directories on the classpath. This should only be done for one classpath module - this is for use when running from source
     * in environments that keep resources and classes in separate locations (e.g. gradle by default). Any directory on the classpath (as opposed to jars) will be
     * included in this module
     *
     * @param metadata          Metadata describing the module to create
     * @param primaryClass      The first representative class to include in the module
     * @param additionalClasses Any additional representative classes to include.
     * @return A new ClasspathModule
     * @throws URISyntaxException If a source location cannot be converted to a proper URI (typically because the path to the source includes an invalid character).
     */
    public static ClasspathModule create(ModuleMetadata metadata, boolean includeDirectories, Class<?> primaryClass, Class<?>... additionalClasses) throws URISyntaxException {
        Set<Path> paths = Sets.newLinkedHashSet();

        for (Class<?> type : Varargs.combineToSet(primaryClass, additionalClasses)) {
            paths.add(Paths.get(type.getProtectionDomain().getCodeSource().getLocation().toURI()));
        }
        if (includeDirectories) {
            addClasspathDirectories(paths);
        }
        return new ClasspathModule(metadata, paths);
    }

    private static void addClasspathDirectories(Set<Path> paths) {
        for (String classpath : System.getProperty("java.class.path").split(File.pathSeparator)) {
            Path path = Paths.get(classpath);
            if (Files.isDirectory(path)) {
                paths.add(path);
            }
        }
    }

    @Override
    public ImmutableList<URL> getClasspaths() {
        return classpaths;
    }

    @Override
    public boolean isOnClasspath() {
        return true;
    }

    @Override
    public boolean isCodeModule() {
        return true;
    }

}
