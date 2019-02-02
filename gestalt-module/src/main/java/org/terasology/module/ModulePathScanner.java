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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.util.Varargs;
import org.terasology.util.io.FileTypesFilter;
import org.terasology.util.io.FilesUtil;

import java.io.IOException;
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
    private final ModuleFactory moduleFactory;

    public ModulePathScanner() {
        this.moduleFactory = new ModuleFactory();
    }

    public ModulePathScanner(ModuleFactory loader) {
        this.moduleFactory = loader;
    }

    public ModuleFactory getModuleFactory() {
        return moduleFactory;
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
        for (Path modulePath : Files.newDirectoryStream(discoveryPath, new FileTypesFilter("jar", "zip"))) {
            loadModule(registry, modulePath);
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
        for (Path modulePath : Files.newDirectoryStream(discoveryPath, Files::isDirectory)) {
            loadModule(registry, modulePath);
        }
    }

    private void loadModule(ModuleRegistry registry, Path modulePath) {
        try {
            Module module = moduleFactory.createModule(modulePath);
            if (registry.add(module)) {
                logger.info("Discovered module: {}", module);
            } else {
                logger.info("Discovered duplicate module: {}-{}, skipping", module.getId(), module.getVersion());
            }
        } catch (IOException e) {
            logger.warn("Failed to load module at '{}'", modulePath, e);
        }
    }
}
