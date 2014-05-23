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

import com.google.common.base.Charsets;
import com.google.gson.JsonIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.util.Varargs;
import org.terasology.util.io.FileTypesFilter;
import org.terasology.util.io.FilesUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A scanner for reading modules off of the filesystem. These modules may either be archives (zip or jar) or directories. To qualify as a module they must contain a
 * json module metadata file (by default named "module.txt").
 *
 * @author Immortius
 */
public class ModulePathScanner {
    private static final Logger logger = LoggerFactory.getLogger(ModulePathScanner.class);
    private String moduleMetadataFilename = "module.txt";
    private ModuleMetadataReader metadataReader = new ModuleMetadataReader();

    /**
     * @return The file name for module metadata.
     */
    public String getModuleMetadataFilename() {
        return moduleMetadataFilename;
    }

    /**
     * @param moduleMetadataFilename The new file name for module metadata
     */
    public void setModuleMetadataFilename(String moduleMetadataFilename) {
        this.moduleMetadataFilename = moduleMetadataFilename;
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
        for (Path discoveryPath : discoveryPaths) {
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
            Path modInfoFile = modulePath.resolve(moduleMetadataFilename);
            if (Files.isRegularFile(modInfoFile)) {
                try (Reader reader = Files.newBufferedReader(modInfoFile, Charsets.UTF_8)) {
                    ModuleMetadata moduleMetadata = metadataReader.read(reader);
                    processModuleInfo(moduleMetadata, modulePath, registry);
                } catch (FileNotFoundException | JsonIOException e) {
                    logger.warn("Failed to load module manifest for module at {}", modulePath, e);
                }
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
        for (Path modulePath : Files.newDirectoryStream(discoveryPath, FilesUtil.DIRECTORY_FILTER)) {
            try (ZipFile zipFile = new ZipFile(modulePath.toFile())) {
                ZipEntry modInfoEntry = zipFile.getEntry(moduleMetadataFilename);
                if (modInfoEntry != null) {
                    try (Reader reader = new InputStreamReader(zipFile.getInputStream(modInfoEntry), Charsets.UTF_8)) {
                        ModuleMetadata moduleMetadata = metadataReader.read(reader);
                        processModuleInfo(moduleMetadata, modulePath, registry);
                    } catch (FileNotFoundException | JsonIOException e) {
                        logger.warn("Failed to load module manifest for module at {}", modulePath, e);
                    }
                }
            } catch (IOException e) {
                logger.error("Invalid module file: {}", modulePath, e);
            }
        }
    }

    /**
     * Processes module information to build a module.
     *
     * @param moduleMetadata The metadata of the module
     * @param modulePath     The location of the module
     * @param registry       The registry of modules to populate
     */
    private void processModuleInfo(ModuleMetadata moduleMetadata, Path modulePath, ModuleRegistry registry) {
        if (moduleMetadata.getId() != null && moduleMetadata.getVersion() != null) {
            Module module = new PathModule(modulePath, moduleMetadata);
            if (registry.add(module)) {
                logger.info("Discovered module: {}", module);
            } else {
                logger.info("Discovered duplicate module: {}-{}, skipping", moduleMetadata.getId(), moduleMetadata.getVersion());
            }
        }
    }
}
