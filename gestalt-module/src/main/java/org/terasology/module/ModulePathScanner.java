// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.module;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.util.Varargs;
import org.terasology.util.io.FileTypesFilter;
import org.terasology.util.io.FilesUtil;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

/**
 * A scanner for reading modules off of the filesystem. These modules may either be archives (zip or jar) or directories. To qualify as a module they must contain a
 * json module metadata file (by default named "module.txt").
 *
 * @author Immortius
 */
public class ModulePathScanner {
    private static final Logger logger = LoggerFactory.getLogger(ModulePathScanner.class);
    private final ModuleLoader moduleLoader;

    public ModulePathScanner() {
        this.moduleLoader = new ModuleLoader();
    }

    public ModulePathScanner(ModuleLoader loader) {
        this.moduleLoader = loader;
    }

    public ModuleLoader getModuleLoader() {
        return moduleLoader;
    }

    /**
     * Scans one or more paths for modules. Paths are scanned in order, with directories scanned before files. If a module is discovered multiple times (same id and version),
     * the first copy of the module found is used.
     *
     * @param registry        The registry to populate with discovered modules
     * @param path            The first path to scan
     * @param additionalPaths Additional paths to scan
     */
    public void scan(ModuleRegistry registry, Path path, Path... additionalPaths) {
        Set<Path> discoveryPaths = Varargs.combineToSet(path, additionalPaths);
        scan(registry, discoveryPaths);
    }

    /**
     * Scans a collection of paths for modules.
     * Paths are scanned in order, with directories scanned before files. If a module is discovered multiple times (same id and version), the first copy of the module
     * found is used.
     *
     * @param registry The registry to populate with discovered modules
     * @param paths    The paths to scan
     */
    public void scan(ModuleRegistry registry, Collection<Path> paths) {
        for (Path discoveryPath : paths) {
            try {
                scanModuleDirectories(discoveryPath, registry);
                scanModuleArchives(discoveryPath, registry);
            } catch (IOException e) {
                logger.error("Failed to scan path {}", discoveryPath, e);
            }
        }
    }

    /**
     * Scans a directory for module archives (jar or zip)
     *
     * @param discoveryPath The directory to scan
     * @param registry      The registry to populate with discovered modules
     * @throws IOException If an error occurs scanning the directory - but not an individual module.
     */
    private void scanModuleArchives(Path discoveryPath, ModuleRegistry registry) throws IOException {
        try (DirectoryStream<Path> discoveryDirectory = Files.newDirectoryStream(discoveryPath,
                new FileTypesFilter("jar", "zip"))) {
            for (Path modulePath : discoveryDirectory) {
                loadModule(registry, modulePath);
            }
        }
    }

    /**
     * Scans a directory for module directories
     *
     * @param discoveryPath The directory to scan
     * @param registry      The registry to populate with discovered modules
     * @throws IOException If an error occurs scanning the directory - but not an individual module.
     */
    private void scanModuleDirectories(Path discoveryPath, ModuleRegistry registry) throws IOException {
        try (DirectoryStream<Path> discoveryDirectory = Files.newDirectoryStream(discoveryPath,
                FilesUtil.DIRECTORY_FILTER)) {
            for (Path modulePath : discoveryDirectory) {
                loadModule(registry, modulePath);
            }
        }
    }

    private void loadModule(ModuleRegistry registry, Path modulePath) {
        try {
            Module module = moduleLoader.load(modulePath);
            if (module != null) {
                if (registry.add(module)) {
                    logger.info("Discovered module: {}", module);
                } else {
                    logger.info("Discovered duplicate module: {}-{}, skipping", module.getId(), module.getVersion());
                }
            } else {
                logger.warn("Not a module: {}", modulePath);
            }
        } catch (IOException e) {
            logger.warn("Failed to load module at '{}'", modulePath, e);
        }
    }
}
