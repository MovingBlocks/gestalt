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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Helper class for loading modules from a file location (directory or archive file).
 *
 * @author Immortius
 */
public class ModuleLoader {

    private Path moduleInfoPath = Paths.get("module.info");
    private Path directoryCodeLocation = Paths.get("build", "classes");
    private final ModuleMetadataReader metadataReader;

    public ModuleLoader() {
        metadataReader = new ModuleMetadataReader();
    }

    /**
     * @param metadataReader The metadata reader to use when loading module metadata.
     */
    public ModuleLoader(ModuleMetadataReader metadataReader) {
        this.metadataReader = metadataReader;
    }

    /**
     * @return The location within a module where the metadata can be found
     */
    public Path getModuleInfoPath() {
        return moduleInfoPath;
    }

    /**
     * @param moduleInfoFilename The location within a module where the metadata can be found
     */
    public void setModuleInfoPath(Path moduleInfoFilename) {
        this.moduleInfoPath = moduleInfoFilename;
    }

    /**
     * @return The location in a directory module where code can be found
     */
    public Path getDirectoryCodeLocation() {
        return directoryCodeLocation;
    }

    /**
     * @param directoryCodeLocation The location in a directory module where code can be found
     */
    public void setDirectoryCodeLocation(Path directoryCodeLocation) {
        this.directoryCodeLocation = directoryCodeLocation;
    }

    /**
     * @param modulePath The path to the module to load
     * @return The loaded module, or null if the path is not a module (contains no module metadata)
     * @throws IOException If an error occurs loading the module
     */
    public Module load(Path modulePath) throws IOException {
        Preconditions.checkArgument(Files.exists(modulePath), "Module does not exist at: " + modulePath);
        if (Files.isDirectory(modulePath)) {
            return loadDirectoryModule(modulePath);
        } else if (Files.isRegularFile(modulePath)) {
            return loadArchiveModule(modulePath);
        }
        return null;
    }

    private Module loadArchiveModule(Path modulePath) throws IOException {
        try (ZipFile zipFile = new ZipFile(modulePath.toFile())) {
            ZipEntry modInfoEntry = zipFile.getEntry(moduleInfoPath.toString());
            if (modInfoEntry != null) {
                try (Reader reader = new InputStreamReader(zipFile.getInputStream(modInfoEntry), Charsets.UTF_8)) {
                    return new ArchiveModule(modulePath, metadataReader.read(reader));
                }
            }
        }
        return null;
    }

    private Module loadDirectoryModule(Path modulePath) throws IOException {
        Path modInfoFile = modulePath.resolve(moduleInfoPath);
        if (Files.isRegularFile(modInfoFile)) {
            try (Reader reader = Files.newBufferedReader(modInfoFile, Charsets.UTF_8)) {
                return new PathModule(modulePath, modulePath.resolve(directoryCodeLocation), metadataReader.read(reader));
            }
        }
        return null;
    }
}
