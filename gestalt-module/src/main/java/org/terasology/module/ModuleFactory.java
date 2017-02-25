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

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonParseException;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.module.exceptions.InvalidModulePathException;
import org.terasology.util.Varargs;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A factory for creating various configurations of modules.
 */
public class ModuleFactory {

    private static final Logger logger = LoggerFactory.getLogger(ModuleFactory.class);

    private Path defaultCodeSubpath = Paths.get("build", "classes");
    private Path defaultLibsSubpath = Paths.get("libs");
    private final Map<Path, ModuleMetadataLoader> moduleMetadataLoaderMap = Maps.newLinkedHashMap();
    private Path reflectionsCachePath = Paths.get("reflections.cache");

    public ModuleFactory() {
        moduleMetadataLoaderMap.put(Paths.get("module.json"), new ModuleMetadataJsonAdapter());
    }

    /**
     * @param defaultCodeSubpath The default subpath in a path module that contains code (compiled class files)
     * @param defaultLibsSubpath The default subpath in a path module that contains libraries (jars)
     */
    public ModuleFactory(Path defaultCodeSubpath, Path defaultLibsSubpath) {
        this(defaultCodeSubpath, defaultLibsSubpath, ImmutableMap.of(Paths.get("module.json"), new ModuleMetadataJsonAdapter()));
    }

    /**
     * @param defaultCodeSubpath The default subpath in a path module that contains code (compiled class files)
     * @param defaultLibsSubpath The default subpath in a path module that contains libraries (jars)
     * @param metadataLoaders    A map of relative paths/files to metadata loaders to use for loading module metadata
     */
    public ModuleFactory(Path defaultCodeSubpath, Path defaultLibsSubpath, Map<Path, ModuleMetadataLoader> metadataLoaders) {
        this.moduleMetadataLoaderMap.putAll(metadataLoaders);
        this.defaultCodeSubpath = defaultCodeSubpath;
        this.defaultLibsSubpath = defaultLibsSubpath;
    }

    /**
     * @return The subpath of a path module that contains compiled code
     */
    public Path getDefaultCodeSubpath() {
        return defaultCodeSubpath;
    }

    /**
     * Sets the default subpath for code in a path module
     *
     * @param defaultCodeSubpath
     */
    public void setDefaultCodeSubpath(Path defaultCodeSubpath) {
        this.defaultCodeSubpath = defaultCodeSubpath;
    }

    /**
     * @return The subpath of a path module that contains libraries
     */
    public Path getDefaultLibsSubpath() {
        return defaultLibsSubpath;
    }

    /**
     * Sets the default subpath for libraries in a path module
     *
     * @param defaultLibsSubpath
     */
    public void setDefaultLibsSubpath(Path defaultLibsSubpath) {
        this.defaultLibsSubpath = defaultLibsSubpath;
    }

    /**
     * @return The map of paths to module metadata loaders used for loading metadata describing modules
     */
    public Map<Path, ModuleMetadataLoader> getModuleMetadataLoaderMap() {
        return moduleMetadataLoaderMap;
    }

    /**
     * @return T
     */
    public Path getReflectionsCachePath() {
        return reflectionsCachePath;
    }

    /**
     * Sets the path of the reflections cache file that will be loaded from modules.
     *
     * @param reflectionsCachePath
     */
    public void setReflectionsCachePath(Path reflectionsCachePath) {
        this.reflectionsCachePath = reflectionsCachePath;
    }

    /**
     * Creates a module for a path, depending on whether it is a directory or a file.
     *
     * @param path     The path to create a module for.
     * @param metadata The metadata describing the module.
     * @return The new module.
     */
    public Module createModule(Path path, ModuleMetadata metadata) {
        if (Files.isDirectory(path)) {
            return createPathModule(path, metadata);
        } else {
            return createArchiveModule(path, metadata);
        }
    }

    /**
     * Creates a module from a path, determining whether it is an archive (jar or zip) or directory and handling it appropriately. A module metadata file will be loaded and
     * to determine the module's id, version and other details.
     * @param path
     * @return The loaded module
     * @throws IOException If the module fails to load, including if the module metadata file cannot be found or loaded.
     */
    public Module createModule(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            return createPathModule(path);
        } else {
            return createArchiveModule(path);
        }
    }

    /**
     * Creates a path module from a directory, loading metadata from an available metadata file.
     * @param path
     * @return The created module
     * @throws IOException If the module fails to load, including if the module metadata file cannot be found or loaded.
     */
    public Module createPathModule(Path path) throws IOException {
        for (Map.Entry<Path, ModuleMetadataLoader> entry : moduleMetadataLoaderMap.entrySet()) {
            Path modInfoFile = path.resolve(entry.getKey());
            if (Files.isRegularFile(modInfoFile)) {
                try (Reader reader = Files.newBufferedReader(modInfoFile, Charsets.UTF_8)) {
                    return createPathModule(path, entry.getValue().read(reader));
                }
            }
        }
        throw new IOException("Could not resolve module metadata for module at " + path);
    }

    /**
     * Creates an archive module from a file, loading metadata from an available metadata file inside the archive.
     * @param modulePath
     * @return The created module
     * @throws IOException If the module fails to load, including if the module metadata file cannot be found or loaded.
     */
    public Module createArchiveModule(Path modulePath) throws IOException {
        try (ZipFile zipFile = new ZipFile(modulePath.toFile())) {
            for (Map.Entry<Path, ModuleMetadataLoader> entry : moduleMetadataLoaderMap.entrySet()) {
                ZipEntry modInfoEntry = zipFile.getEntry(entry.getKey().toString());
                if (modInfoEntry != null) {
                    try (Reader reader = new InputStreamReader(zipFile.getInputStream(modInfoEntry), Charsets.UTF_8)) {
                        try {
                            ModuleMetadata metadata = entry.getValue().read(reader);
                            return createArchiveModule(modulePath, metadata);
                        } catch (JsonParseException e) {
                            throw new IOException("Failed to read metadata for module " + modulePath, e);
                        }
                    }
                }
            }
        }
        throw new IOException("Missing module metadata in archive module '" + modulePath + "'");
    }

    /**
     * Creates a module for a directory. This module will use the default code and libs subpaths.
     *
     * @param path     The path of the directory to make a module for
     * @param metadata The metadata describing the module
     * @return A new module for the given directory
     */
    public Module createPathModule(Path path, ModuleMetadata metadata) {
        return createPathModule(path, path.resolve(defaultCodeSubpath), path.resolve(defaultLibsSubpath), metadata);
    }

    /**
     * Creates a module for a directory.
     *
     * @param path     The path of the directory to make a module for
     * @param codePath The path of code to add to the classpath of the module
     * @param libsPath The path of any jars to add to the classpath of the module
     * @param metadata The metadata describing the module
     * @return A new module for the given directory
     */
    public Module createPathModule(Path path, Path codePath, Path libsPath, ModuleMetadata metadata) {
        Preconditions.checkArgument(Files.isDirectory(path));

        ImmutableList.Builder<URL> classpathBuilder = ImmutableList.builder();
        if (Files.isDirectory(codePath)) {
            try {
                classpathBuilder.add(codePath.toUri().toURL());
            } catch (MalformedURLException e) {
                throw new InvalidModulePathException("Unable to convert path to URL: " + codePath, e);
            }
        }
        if (Files.isDirectory(libsPath)) {
            try (DirectoryStream<Path> libs = Files.newDirectoryStream(libsPath)) {
                for (Path lib : libs) {
                    if (Files.isRegularFile(lib)) {
                        classpathBuilder.add(lib.toUri().toURL());
                    }
                }
            } catch (IOException e) {
                throw new InvalidModulePathException("Error reading libs", e);
            }
        }

        Reflections reflectionsCache = readReflectionsCacheFromPath(path, null);

        return new Module(Collections.singletonList(path), classpathBuilder.build(), metadata, reflectionsCache);
    }

    /**
     * Creates an archive module for a jar or zip file module.
     *
     * @param path     The path of the module - must be a file.
     * @param metadata The metadata describing the module.
     * @return A new module for the archive.
     */
    public Module createArchiveModule(Path path, ModuleMetadata metadata) {
        Preconditions.checkArgument(Files.isRegularFile(path));
        try {
            Reflections reflectionsCache = readReflectionsCacheFromArchive(path, null);
            return new Module(Collections.singletonList(path), ImmutableList.of(path.toUri().toURL()), metadata, reflectionsCache);
        } catch (MalformedURLException e) {
            throw new InvalidModulePathException("Could not convert path to URL: " + path, e);
        }
    }

    /**
     * Creates a module for assets that are on the classpath - that is, they are already loaded and available in the root java context.
     *
     * @param paths    The paths for jars and code paths to include in the module
     * @param metadata Metadata describing the module.
     * @return A new module for the specified locations of the classpath.
     */
    public Module createClasspathModule(Collection<Path> paths, ModuleMetadata metadata) {
        logger.debug("Creating ClasspathModule '{}' with paths '{}'", metadata.getId(), paths);
        Reflections reflectionsCache = null;
        ImmutableList.Builder<URL> builder = ImmutableList.builder();
        for (Path path : paths) {
            try {
                builder.add(path.toUri().toURL());
                if (Files.isDirectory(path)) {
                    reflectionsCache = readReflectionsCacheFromPath(path, reflectionsCache);
                } else {
                    reflectionsCache = readReflectionsCacheFromArchive(path, reflectionsCache);
                }
            } catch (MalformedURLException e) {
                throw new InvalidModulePathException("Path cannot be converted to URL: " + path, e);
            }
        }
        return new Module(paths, builder.build(), true, metadata, reflectionsCache);
    }

    /**
     * Creates a classpath module from a set of code sources.
     * <p>
     * There is an option to include directories on the classpath. This should only be done for one classpath module - this is for use when running from source
     * in environments that keep resources and classes in separate locations (e.g. gradle by default). Any directory on the classpath (as opposed to jars) will be
     * included in this module
     * </p>
     *
     * @param metadata           Metadata describing the module to create
     * @param includeDirectories Whether to include directories on the classpath.
     * @param primarySource      The first source to include in this module
     * @param additionalSources  Any additional sources to include
     * @return A new module describing the provided source locations.
     * @throws URISyntaxException If a source location cannot be converted to a proper URI (typically because the path to the source includes an invalid character).
     */
    public Module createClasspathModule(ModuleMetadata metadata, boolean includeDirectories, CodeSource primarySource, CodeSource... additionalSources)
            throws URISyntaxException {
        Set<Path> paths = Sets.newLinkedHashSet();

        for (CodeSource source : Varargs.combineToSet(primarySource, additionalSources)) {
            paths.add(Paths.get(source.getLocation().toURI()));
        }
        if (includeDirectories) {
            addClasspathDirectories(paths);
        }
        return createClasspathModule(paths, metadata);
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
    public Module createClasspathModule(ModuleMetadata metadata, Class<?> primaryClass, Class<?>... additionalClasses) throws URISyntaxException {
        return createClasspathModule(metadata, false, primaryClass, additionalClasses);
    }

    /**
     * Creates a classpath module from a set of representative classes. The code source (e.g. Jar or directory) for each class is included in the Classpath module.
     * <p>
     * There is an option to include directories on the classpath. This should only be done for one classpath module - this is for use when running from source
     * in environments that keep resources and classes in separate locations (e.g. gradle by default). Any directory on the classpath (as opposed to jars) will be
     * included in this module
     * </p>
     *
     * @param metadata           Metadata describing the module to create
     * @param includeDirectories Should directories on the classpath be included in this module?
     * @param primaryClass       The first representative class to include in the module
     * @param additionalClasses  Any additional representative classes to include.
     * @return A new ClasspathModule
     * @throws URISyntaxException If a source location cannot be converted to a proper URI (typically because the path to the source includes an invalid character).
     */
    public Module createClasspathModule(ModuleMetadata metadata, boolean includeDirectories, Class<?> primaryClass, Class<?>... additionalClasses) throws URISyntaxException {
        Set<Path> paths = Sets.newLinkedHashSet();

        for (Class<?> type : Varargs.combineToSet(primaryClass, additionalClasses)) {
            paths.add(Paths.get(type.getProtectionDomain().getCodeSource().getLocation().toURI()));
        }
        if (includeDirectories) {
            addClasspathDirectories(paths);
        }
        return createClasspathModule(paths, metadata);
    }

    private void addClasspathDirectories(Set<Path> paths) {
        for (String classpath : System.getProperty("java.class.path").split(File.pathSeparator)) {
            Path path = Paths.get(classpath);
            if (Files.isDirectory(path)) {
                paths.add(path);
            }
        }
    }

    private Reflections readReflectionsCacheFromPath(Path path, Reflections reflectionsCache) {
        Path reflectionsCacheFile = path.resolve(reflectionsCachePath);
        if (Files.isRegularFile(reflectionsCacheFile)) {
            try (InputStream stream = new BufferedInputStream(Files.newInputStream(path.resolve(reflectionsCachePath)))) {
                if (reflectionsCache == null) {
                    reflectionsCache = new ConfigurationBuilder().getSerializer().read(stream);
                } else {
                    reflectionsCache.collect(stream);
                }
            } catch (IOException e) {
                logger.error("Failure attempting to read reflections cache from {}", path, e);
            }
        }
        return reflectionsCache;
    }

    private Reflections readReflectionsCacheFromArchive(Path path, Reflections reflectionsCache) {
        try (ZipFile zipFile = new ZipFile(path.toFile())) {
            ZipEntry modInfoEntry = zipFile.getEntry(reflectionsCachePath.toString());
            if (modInfoEntry != null) {
                try (InputStream stream = zipFile.getInputStream(modInfoEntry)) {
                    if (reflectionsCache == null) {
                        reflectionsCache = new ConfigurationBuilder().getSerializer().read(stream);
                    } else {
                        reflectionsCache.collect(stream);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Failure attempting to read reflections cache from {}", this, e);
        }
        return reflectionsCache;
    }

}
