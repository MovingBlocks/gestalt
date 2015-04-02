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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.terasology.module.Module;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A file system providing access to the contents of a Module.
 * <p/>
 * A ModuleFileSystem has a single root '/', and separates each directory and file with '/'.
 * Modification and write operations are not supported. WatchService is supported though, for detecting external changes to a module - this is only
 * supported to changes happening on the default filesystem (so directories, not in archives).
 *
 * @author Immortius
 */
class ModuleFileSystem extends FileSystem {

    private static final Set<String> SUPPORTED_FILE_ATTRIBUTE_VIEWS = ImmutableSet.of("basic");

    private final ModuleFileSystemProvider provider;
    private final Module module;
    private Map<Path, FileSystem> openedFileSystems = Maps.newConcurrentMap();
    private boolean open = true;

    ModuleFileSystem(ModuleFileSystemProvider provider, Module module) {
        this.provider = provider;
        this.module = module;
    }

    public Module getModule() {
        return module;
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
        return Arrays.<Path>asList(getPath("/"));
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
                    return new PathMatcher() {
                        @Override
                        public boolean matches(Path path) {
                            return pattern.matcher(path.toString()).matches();
                        }
                    };
                }
                case "glob": {
                    final Pattern pattern = Pattern.compile(GlobSupport.globToRegex(parts[1]));
                    return new PathMatcher() {
                        @Override
                        public boolean matches(Path path) {
                            return pattern.matcher(path.toString()).matches();
                        }
                    };
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

    FileSystem getContainedFileSystem(Path location) throws IOException {
        Preconditions.checkArgument(module.getLocations().contains(location), "Location not contained in module");

        FileSystem containedFileSystem = openedFileSystems.get(location);
        if (containedFileSystem == null) {
            containedFileSystem = FileSystems.newFileSystem(location, null);
            openedFileSystems.put(location, containedFileSystem);
        }
        return containedFileSystem;
    }
}
