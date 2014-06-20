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
import org.terasology.module.exceptions.InvalidModulePathException;
import org.terasology.util.Varargs;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
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

    private final ImmutableList<URL> classpaths;

    /**
     * @param metadata Module metadata describing this module
     * @param paths    a collection of paths to the module locations - may be a mixture of file and directory paths
     */
    private ClasspathModule(ModuleMetadata metadata, Collection<Path> paths) {
        super(paths, metadata);
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
     * Creates a classpath module from a set of code sources
     *
     * @param metadata          Metadata describing the module to create
     * @param primarySource     The first source to include in this module
     * @param additionalSources Any additional sources to include
     * @return A new ClasspathModule
     * @throws URISyntaxException If a source location cannot be converted to a proper URI (typically because the path to the source includes an invalid character).
     */
    public static ClasspathModule create(ModuleMetadata metadata, CodeSource primarySource, CodeSource... additionalSources) throws URISyntaxException {
        Set<Path> paths = Sets.newLinkedHashSet();
        for (CodeSource source : Varargs.combineToSet(primarySource, additionalSources)) {
            paths.add(Paths.get(source.getLocation().toURI()));
        }
        return new ClasspathModule(metadata, paths);
    }

    /**
     * Creates a classpath module from a set of representative classes. The code source (e.g. Jar or directory) for each class is included in the Classpath module
     *
     * @param metadata          Metadata describing the module to create
     * @param primaryClass      The first representative class to include in the module
     * @param additionalClasses Any additional representative classes to include.
     * @return A new ClasspathModule
     * @throws URISyntaxException If a source location cannot be converted to a proper URI (typically because the path to the source includes an invalid character).
     */
    public static ClasspathModule create(ModuleMetadata metadata, Class<?> primaryClass, Class<?>... additionalClasses) throws URISyntaxException {
        Set<Path> paths = Sets.newLinkedHashSet();
        for (Class<?> type : Varargs.combineToSet(primaryClass, additionalClasses)) {
            paths.add(Paths.get(type.getProtectionDomain().getCodeSource().getLocation().toURI()));
        }
        return new ClasspathModule(metadata, paths);
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
