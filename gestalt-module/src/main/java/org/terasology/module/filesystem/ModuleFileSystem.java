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

package org.terasology.module.filesystem;

import android.support.annotation.RequiresApi;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.terasology.module.ModuleEnvironment;
import org.terasology.util.Varargs;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A file system providing access to the contents of a Module environment.
 * <p>
 * A ModuleFileSystem has a root for each module ('/moduleName'), and separates each directory and file with '/'.
 * Modification and write operations are not supported. WatchService is supported though, for detecting external changes - this is only
 * supported to changes happening on the default filesystem (so directories, not in archives).
 *
 * @author Immortius
 */
@RequiresApi(26)
class ModuleFileSystem extends FileSystem {

    private static final Set<String> SUPPORTED_FILE_ATTRIBUTE_VIEWS = ImmutableSet.of("basic");

    private final ModuleFileSystemProvider provider;
    private final ModuleEnvironment environment;
    private Map<Path, FileSystem> openedFileSystems = Maps.newConcurrentMap();
    private boolean open = true;

    ModuleFileSystem(ModuleFileSystemProvider provider, ModuleEnvironment environment) {
        this.provider = provider;
        this.environment = environment;
    }

    public ModuleEnvironment getEnvironment() {
        return environment;
    }

    @Override
    public FileSystemProvider provider() {
        return provider;
    }

    @Override
    public void close() throws IOException {
        open = false;
        provider.removeFileSystem(this);
        for (FileSystem openedFileSystem : openedFileSystems.values()) {
            openedFileSystem.close();
        }
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public String getSeparator() {
        return ModuleFileSystemProvider.SEPARATOR;
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return environment.getModulesOrderedByDependencies().stream()
                .<Path>map(module -> getPath("/", module.getId().toString())).collect(Collectors.toList());
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return SUPPORTED_FILE_ATTRIBUTE_VIEWS;
    }

    @Override
    public ModulePath getPath(String first, String... more) {
        List<String> parts = Varargs.combineToList(first, more);
        StringBuilder builder = new StringBuilder();

        for (String part : parts) {
            String trimmedPart = part.trim();
            if (!trimmedPart.isEmpty()) {
                if (trimmedPart.charAt(0) == ModuleFileSystemProvider.SEPARATOR.charAt(0)) {
                    builder.append(ModuleFileSystemProvider.SEPARATOR);
                }
                break;
            }
        }

        for (String part : parts) {
            for (String subPart : part.split(ModuleFileSystemProvider.SEPARATOR, 0)) {
                if (!subPart.isEmpty()) {
                    if (builder.length() > 0 && builder.charAt(builder.length() - 1) != ModuleFileSystemProvider.SEPARATOR.charAt(0)) {
                        builder.append(ModuleFileSystemProvider.SEPARATOR);
                    }
                    builder.append(subPart);
                }
            }
        }
        return new ModulePath(builder.toString(), this);
    }

    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        String[] parts = syntaxAndPattern.split(":", 2);
        if (parts.length == 2) {
            switch (parts[0]) {
                case "regex": {
                    final Pattern pattern = Pattern.compile(parts[1]);
                    return path -> pattern.matcher(path.toString()).matches();
                }
                case "glob": {
                    final Pattern pattern = Pattern.compile(GlobSupport.globToRegex(parts[1]));
                    return path -> pattern.matcher(path.toString()).matches();
                }
                default:
                    throw new UnsupportedOperationException("Syntax '" + parts[0] + "' not recognized");
            }
        } else {
            throw new IllegalArgumentException("Invalid format: '" + syntaxAndPattern + "'");
        }
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException();
    }

    @Override
    public WatchService newWatchService() throws IOException {
        return new ModuleWatchService(this);
    }

    FileSystem getRealFileSystem(Path moduleLocation) throws IOException {
        FileSystem containedFileSystem = openedFileSystems.get(moduleLocation);
        if (containedFileSystem == null) {
            containedFileSystem = FileSystems.newFileSystem(moduleLocation, null);
            openedFileSystems.put(moduleLocation, containedFileSystem);
        }
        return containedFileSystem;
    }
}
