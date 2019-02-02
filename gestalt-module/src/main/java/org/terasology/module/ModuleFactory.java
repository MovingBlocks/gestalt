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

package org.terasology.module;

import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.reflect.Reflection;
import com.google.gson.JsonParseException;

import org.reflections.Configuration;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.serializers.JsonSerializer;
import org.reflections.serializers.Serializer;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.module.resources.ArchiveFileSource;
import org.terasology.module.resources.ClasspathFileSource;
import org.terasology.module.resources.DirectoryFileSource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
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
    private static final Configuration EMPTY_CONFIG = new ConfigurationBuilder();
    private static final String STANDARD_CODE_SUBPATH = "build/classes";
    private static final String STANDARD_LIBS_SUBPATH = "libs";
    private final Map<String, ModuleMetadataLoader> moduleMetadataLoaderMap = Maps.newLinkedHashMap();
    private final Map<String, Serializer> manifestSerializersByFilename = Maps.newLinkedHashMap();
    private final ClassLoader classLoader;
    private String defaultCodeSubpath;
    private String defaultLibsSubpath;
    private boolean scanningForClasses = true;

    public ModuleFactory() {
        this(ClasspathHelper.contextClassLoader());
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
        this(ClasspathHelper.contextClassLoader(), defaultCodeSubpath, defaultLibsSubpath, ImmutableMap.of("module.json", new ModuleMetadataJsonAdapter()));
    }

    /**
     * @param defaultCodeSubpath The default subpath in a path module that contains code (compiled class files)
     * @param defaultLibsSubpath The default subpath in a path module that contains libraries (jars)
     * @param metadataLoaders    A map of relative paths/files to metadata loaders to use for loading module metadata
     */
    public ModuleFactory(ClassLoader classLoader, String defaultCodeSubpath, String defaultLibsSubpath, Map<String, ModuleMetadataLoader> metadataLoaders) {
        this.classLoader = classLoader;
        this.moduleMetadataLoaderMap.putAll(metadataLoaders);
        this.defaultCodeSubpath = defaultCodeSubpath;
        this.defaultLibsSubpath = defaultLibsSubpath;
        manifestSerializersByFilename.put("manifest.json", new JsonSerializer());
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
     * @param defaultCodeSubpath
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
     * @param defaultLibsSubpath
     */
    public void setDefaultLibsSubpath(String defaultLibsSubpath) {
        this.defaultLibsSubpath = defaultLibsSubpath;
    }

    /**
     * Adds a deserializer for a manifest file.
     *
     * @param name         The name of the manifest file this loader will load
     * @param deserializer The deserializer
     */
    public void setManifestFileType(String name, Serializer deserializer) {
        this.manifestSerializersByFilename.put(name, deserializer);
    }

    /**
     * @return The map of paths to module metadata loaders used for loading metadata describing modules
     */
    public Map<String, ModuleMetadataLoader> getModuleMetadataLoaderMap() {
        return moduleMetadataLoaderMap;
    }

    @NonNull
    private Reflections scanOrLoadClasspathReflections(String packagePath) {
        String path = packagePath;
        if (!path.isEmpty() && !path.endsWith("/")) {
            path += "/";
        }
        Reflections manifest = new Reflections(EMPTY_CONFIG);
        try {
            boolean loaded = false;
            for (Map.Entry<String, Serializer> manifestEntry : manifestSerializersByFilename.entrySet()) {
                Enumeration<URL> resources = classLoader.getResources(path + manifestEntry.getKey());
                while (resources.hasMoreElements()) {
                    try (InputStream stream = resources.nextElement().openStream()) {
                        manifest.merge(manifestEntry.getValue().read(stream));
                        loaded = true;
                    }
                }
            }
            if (!loaded && scanningForClasses) {
                Configuration config = new ConfigurationBuilder().addScanners(new ResourcesScanner(), new SubTypesScanner(false), new TypeAnnotationsScanner()).addClassLoader(classLoader).forPackages(packagePath);
                Reflections reflections = new Reflections(config);
                manifest.merge(reflections);
            }

        } catch (IOException e) {
            logger.error("Failed to gather class manifest for classpath module", e);
        }
        return manifest;
    }

    @NonNull
    private Reflections scanOrLoadDirectoryManifest(File directory) {
        Reflections manifest = new Reflections(EMPTY_CONFIG);
        try {
            boolean loaded = false;
            for (Map.Entry<String, Serializer> manifestEntry : manifestSerializersByFilename.entrySet()) {
                File manifestFile = new File(directory, manifestEntry.getKey());
                if (manifestFile.exists() && manifestFile.isFile()) {
                    try (InputStream stream = new FileInputStream(manifestFile)) {
                        manifest.merge(manifestEntry.getValue().read(stream));
                        loaded = true;
                    }
                }
            }
            if (!loaded) {
                scanContents(directory, manifest);
            }

        } catch (IOException e) {
            logger.error("Failed to gather class manifest for classpath module", e);
        }
        return manifest;
    }

    private void scanContents(File directory, Reflections manifest) throws MalformedURLException {
        if (scanningForClasses) {
            Configuration config = new ConfigurationBuilder().addScanners(new ResourcesScanner(), new SubTypesScanner(false), new TypeAnnotationsScanner()).addUrls(directory.toURI().toURL());
            Reflections reflections = new Reflections(config);
            manifest.merge(reflections);
        }
    }

    @NonNull
    private Reflections scanOrLoadArchiveManifest(File archive) {
        Reflections manifest = new Reflections(EMPTY_CONFIG);
        try {
            boolean loaded = false;
            try (ZipFile zipFile = new ZipFile(archive)) {
                for (Map.Entry<String, Serializer> manifestEntry : manifestSerializersByFilename.entrySet()) {
                    ZipEntry modInfoEntry = zipFile.getEntry(manifestEntry.getKey());
                    if (modInfoEntry != null && !modInfoEntry.isDirectory()) {
                        try (InputStream stream = zipFile.getInputStream(modInfoEntry)) {
                            manifest.merge(manifestEntry.getValue().read(stream));
                            loaded = true;
                        }
                    }
                }
            }

            if (!loaded) {
                scanContents(archive, manifest);
            }
        } catch (IOException e) {
            logger.error("Failed to gather class manifest for classpath module", e);
        }
        return manifest;
    }

    /**
     * Creates a module from a package on the main classpath.
     *
     * @param metadata    The metadata describing the module
     * @param packageName The package to create the module from, as a list of the parts of the package
     * @return A module covering the contents of the package on the classpath
     * @
     */
    public Module createPackageModule(ModuleMetadata metadata, String packageName) {
        List<String> packageParts = Arrays.asList(packageName.split(Pattern.quote(".")));
        Reflections manifest = scanOrLoadClasspathReflections(RESOURCE_PATH_JOINER.join(packageParts));

        return new Module(metadata, new ClasspathFileSource(manifest, RESOURCE_PATH_JOINER.join(packageParts), classLoader), Collections.emptyList(), manifest, x -> {
            String classPackageName = Reflection.getPackageName(x);
            return packageName.equals(classPackageName) || classPackageName.startsWith(packageName + ".");
        });
    }

    /**
     * Creates a module from a directory.
     *
     * @param directory The directory to load as a module
     * @return A module covering the contents of the directory
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

        Reflections manifest = new Reflections(EMPTY_CONFIG);
        List<File> codeLocations = Lists.newArrayList();
        File codeDir = new File(directory, getDefaultCodeSubpath());
        if (codeDir.exists() && codeDir.isDirectory()) {
            codeLocations.add(codeDir);
            manifest.merge(scanOrLoadDirectoryManifest(codeDir));
        }
        File libDir = new File(directory, getDefaultLibsSubpath());
        if (libDir.exists() && libDir.isDirectory() && libDir.listFiles() != null) {
            File[] libDirContents = libDir.listFiles();
            if (libDirContents != null) {
                for (File lib : libDirContents) {
                    if (lib.isFile()) {
                        codeLocations.add(lib);
                        manifest.merge(scanOrLoadArchiveManifest(lib));
                    }
                }
            }
        }

        return new Module(metadata, new DirectoryFileSource(directory), codeLocations, manifest, x -> false);
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
            return new Module(metadata, new ArchiveFileSource(archive), Collections.singletonList(archive), scanOrLoadArchiveManifest(archive), x -> false);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Unable to convert file path to url for " + archive, e);
        }
    }

    /**
     * Creates a module for a path, depending on whether it is a directory or a file.
     *
     * @param path     The path to create a module for.
     * @param metadata The metadata describing the module.
     * @return The new module.
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
     * @param path
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
