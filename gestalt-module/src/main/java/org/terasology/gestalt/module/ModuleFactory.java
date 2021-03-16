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

import android.support.annotation.RequiresApi;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.reflect.Reflection;
import com.google.gson.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.gestalt.di.index.ClassIndex;
import org.terasology.gestalt.di.index.CompoundClassIndex;
import org.terasology.gestalt.di.index.UrlClassIndex;
import org.terasology.gestalt.module.exceptions.InvalidModulePathException;
import org.terasology.gestalt.module.exceptions.MissingModuleMetadataException;
import org.terasology.gestalt.module.resources.ArchiveFileSource;
import org.terasology.gestalt.module.resources.ClasspathFileSource;
import org.terasology.gestalt.module.resources.DirectoryFileSource;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A factory for creating various configurations of modules.
 */
@RequiresApi(24)
public class ModuleFactory {

    private static final Logger logger = LoggerFactory.getLogger(ModuleFactory.class);
    private static final Joiner RESOURCE_PATH_JOINER = Joiner.on('/');
    private static final String STANDARD_CODE_SUBPATH = "build/classes";
    private static final String STANDARD_LIBS_SUBPATH = "libs";
    private final Map<String, ModuleMetadataLoader> moduleMetadataLoaderMap = Maps.newLinkedHashMap();
    private final ClassLoader classLoader;
    private String defaultCodeSubpath;
    private String defaultLibsSubpath;
    private boolean scanningForClasses = true;

    @Inject // TODO use another constructor.
    public ModuleFactory() {
        this(Thread.currentThread().getContextClassLoader());
    }

    /**
     * @param classLoader The classloader to use for classpath and package modules
     */
    public ModuleFactory(ClassLoader classLoader) {
        this(classLoader, STANDARD_CODE_SUBPATH, STANDARD_LIBS_SUBPATH, ImmutableMap.of("module.json", new ModuleMetadataJsonAdapter()));
    }

    /**
     * @param defaultCodeSubpath The default subpath in a path module that contains code (compiled class files)
     * @param defaultLibsSubpath The default subpath in a path module that contains libraries (jars)
     */
    public ModuleFactory(String defaultCodeSubpath, String defaultLibsSubpath) {
        this(Thread.currentThread().getContextClassLoader(), defaultCodeSubpath, defaultLibsSubpath, ImmutableMap.of("module.json", new ModuleMetadataJsonAdapter()));
    }

    /**
     * @param classLoader        The classloader that modules should be loaded atop of
     * @param defaultCodeSubpath The default subpath in a path module that contains code (compiled class files)
     * @param defaultLibsSubpath The default subpath in a path module that contains libraries (jars)
     * @param metadataLoaders    A map of relative paths/files to metadata loaders to use for loading module metadata
     */
    public ModuleFactory(ClassLoader classLoader, String defaultCodeSubpath, String defaultLibsSubpath, Map<String, ModuleMetadataLoader> metadataLoaders) {
        this.classLoader = classLoader;
        this.moduleMetadataLoaderMap.putAll(metadataLoaders);
        this.defaultCodeSubpath = defaultCodeSubpath;
        this.defaultLibsSubpath = defaultLibsSubpath;
    }

    /**
     * @return The subpath of a path module that contains compiled code
     */
    public String getDefaultCodeSubpath() {
        return defaultCodeSubpath;
    }

    /**
     * Sets the default subpath for code in a path module
     *
     * @param defaultCodeSubpath The default code subpath
     */
    public void setDefaultCodeSubpath(String defaultCodeSubpath) {
        this.defaultCodeSubpath = defaultCodeSubpath;
    }

    /**
     * @return Whether the module factory will scan modules for class files if a manifest isn't available
     */
    public boolean isScanningForClasses() {
        return scanningForClasses;
    }

    /**
     * @param scanForClasses Whether the module factory should scan modules for class files if a manifest isn't present
     */
    public void setScanningForClasses(boolean scanForClasses) {
        this.scanningForClasses = scanForClasses;
    }

    /**
     * @return The subpath of a path module that contains libraries
     */
    public String getDefaultLibsSubpath() {
        return defaultLibsSubpath;
    }

    /**
     * Sets the default subpath for libraries in a path module
     *
     * @param defaultLibsSubpath The default libs subpath
     */
    public void setDefaultLibsSubpath(String defaultLibsSubpath) {
        this.defaultLibsSubpath = defaultLibsSubpath;
    }

    /**
     * @return The map of paths to module metadata loaders used for loading metadata describing modules
     */
    public Map<String, ModuleMetadataLoader> getModuleMetadataLoaderMap() {
        return moduleMetadataLoaderMap;
    }

    /**
     * Creates a module from a package on the main classpath.
     *
     * @param packageName The package to create the module from, as a list of the parts of the package
     * @return A module covering the contents of the package on the classpath
     */
    public Module createPackageModule(String packageName) {
        ModuleMetadata metadata = null;
        List<String> packageParts = Arrays.asList(packageName.split(Pattern.quote(".")));
        for (Map.Entry<String, ModuleMetadataLoader> metadataEntries : moduleMetadataLoaderMap.entrySet()) {
            String metadataResource = RESOURCE_PATH_JOINER.join(packageParts) + "/" + metadataEntries.getKey();
            InputStream metadataStream = classLoader.getResourceAsStream(metadataResource);
            if (metadataStream != null) {
                try (Reader reader = new InputStreamReader(metadataStream)) {
                    metadata = metadataEntries.getValue().read(reader);
                    break;
                } catch (IOException e) {
                    logger.error("Failed to read metadata resource {}", metadataResource, e);
                }
            }
        }

        if (metadata != null) {
            return createPackageModule(metadata, packageName);
        }
        throw new MissingModuleMetadataException("Missing or failed to load metadata for package module " + packageName);
    }

    /**
     * Creates a module from a package on the main classpath.
     *
     * @param metadata    The metadata describing the module
     * @param packageName The package to create the module from, as a list of the parts of the package
     * @return A module covering the contents of the package on the classpath
     */
    public Module createPackageModule(ModuleMetadata metadata, String packageName) {
        List<String> packageParts = Arrays.asList(packageName.split(Pattern.quote(".")));
        ClassIndex manifest = UrlClassIndex.byClassLoaderPrefix(packageName);

        return new Module(metadata, new ClasspathFileSource(RESOURCE_PATH_JOINER.join(packageParts), classLoader), Collections.emptyList(), manifest, x -> {
            String classPackageName = Reflection.getPackageName(x);
            return packageName.equals(classPackageName) || classPackageName.startsWith(packageName + ".");
        });
    }

    /**
     * Creates a module from a directory.
     *
     * @param directory The directory to load as a module
     * @return A module covering the contents of the directory
     *
     * @throws IOException if no module metadata cannot be resolved or loaded
     */
    public Module createDirectoryModule(File directory) throws IOException {
        for (Map.Entry<String, ModuleMetadataLoader> entry : moduleMetadataLoaderMap.entrySet()) {
            File modInfoFile = new File(directory, entry.getKey());
            if (modInfoFile.exists()) {
                try (Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(modInfoFile), Charsets.UTF_8))) {
                    return createDirectoryModule(entry.getValue().read(reader), directory);
                } catch (IOException e) {
                    logger.error("Error reading module metadata", e);
                }
            }
        }
        throw new IOException("Could not resolve module metadata for module at " + directory);
    }

    /**
     * Creates a module from a directory.
     *
     * @param metadata  The metadata describing the module
     * @param directory The directory to load as a module
     * @return A module covering the contents of the directory
     */
    public Module createDirectoryModule(ModuleMetadata metadata, File directory) {
        Preconditions.checkArgument(directory.isDirectory());
        CompoundClassIndex classIndex = new CompoundClassIndex();
        List<File> codeLocations = Lists.newArrayList();
        File codeDir = new File(directory, getDefaultCodeSubpath());
        if (codeDir.exists() && codeDir.isDirectory()) {
            codeLocations.add(codeDir);
            classIndex.add(UrlClassIndex.byDirectory(codeDir));
        }
        File libDir = new File(directory, getDefaultLibsSubpath());
        if (libDir.exists() && libDir.isDirectory() && libDir.listFiles() != null) {
            File[] libDirContents = libDir.listFiles();
            if (libDirContents != null) {
                for (File lib : libDirContents) {
                    if (lib.isFile()) {
                        codeLocations.add(lib);
                        classIndex.add(UrlClassIndex.byArchive(lib));
                    }
                }
            }
        }

        return new Module(metadata, new DirectoryFileSource(directory), codeLocations, classIndex, x -> false);
    }

    /**
     * Loads an archive (zip) module. This module may contain compiled code (e.g. could be a jar).
     *
     * @param archive The archive file
     * @return The loaded module
     * @throws IOException If there is an issue loading the module
     */
    public Module createArchiveModule(File archive) throws IOException {
        try (ZipFile zipFile = new ZipFile(archive)) {
            for (Map.Entry<String, ModuleMetadataLoader> entry : moduleMetadataLoaderMap.entrySet()) {
                ZipEntry modInfoEntry = zipFile.getEntry(entry.getKey());
                if (modInfoEntry != null) {
                    try (Reader reader = new InputStreamReader(zipFile.getInputStream(modInfoEntry), Charsets.UTF_8)) {
                        try {
                            ModuleMetadata metadata = entry.getValue().read(reader);
                            return createArchiveModule(metadata, archive);
                        } catch (JsonParseException e) {
                            throw new IOException("Failed to read metadata for module " + archive, e);
                        }
                    }
                }
            }
        }
        throw new IOException("Missing module metadata in archive module '" + archive + "'");
    }

    /**
     * Loads an archive (zip) module. This module may contain compiled code (e.g. could be a jar).
     *
     * @param metadata The metadata describing the module
     * @param archive  The archive file
     * @return The loaded module
     * @throws IOException If there is an issue loading the module
     */
    public Module createArchiveModule(ModuleMetadata metadata, File archive) throws IOException {
        Preconditions.checkArgument(archive.isFile());

        try {
            return new Module(metadata, new ArchiveFileSource(archive), Collections.singletonList(archive), UrlClassIndex.byArchive(archive), x -> false);
        } catch (MalformedURLException e) {
            throw new InvalidModulePathException("Unable to convert file path to url for " + archive, e);
        }
    }

    /**
     * Creates a module for a path, which can be a directory or an archive (zip/jar) file
     *
     * @param path     The path to create a module for.
     * @param metadata The metadata describing the module.
     * @return The new module.
     * @throws IOException if there is an issue reading the module
     */
    public Module createModule(ModuleMetadata metadata, File path) throws IOException {
        if (path.isDirectory()) {
            return createDirectoryModule(metadata, path);
        } else {
            return createArchiveModule(metadata, path);
        }
    }

    /**
     * Creates a module from a path, determining whether it is an archive (jar or zip) or directory and handling it appropriately. A module metadata file will be loaded and
     * to determine the module's id, version and other details.
     *
     * @param path The path locating the module
     * @return The loaded module
     * @throws IOException If the module fails to load, including if the module metadata file cannot be found or loaded.
     */
    public Module createModule(File path) throws IOException {
        Preconditions.checkArgument(path.exists());
        if (path.isDirectory()) {
            return createDirectoryModule(path);
        } else {
            return createArchiveModule(path);
        }
    }

}
