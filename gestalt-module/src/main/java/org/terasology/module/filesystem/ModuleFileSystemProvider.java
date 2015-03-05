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

import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.module.Module;
import org.terasology.module.ModuleRegistry;
import org.terasology.module.TableModuleRegistry;
import org.terasology.naming.Name;
import org.terasology.naming.Version;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A provider for ModuleFileSystems - the factory for ModuleFileSystem. This can be registered to allow module content to be accessed by uri.
 * <p/>
 * The FileSystemProvider in addition to producing FileSystems, also provides the low level methods that drive many file operations.  ModuleFileSystemProvider does this
 * by delegating down to the underlying file systems, translating ModulePaths to real paths and back again as necessary. In situations where a file occurs in multiple
 * locations in a module the first one found is used.
 * <p/>
 * The ModuleFileSystem does support WatchServices/WatchKeys, but only for locations on the default file system. If no such location exists then WatchKeys produced will be
 * return false from isValid on creation.
 *
 * @author Immortius
 */
public class ModuleFileSystemProvider extends FileSystemProvider {

    public static final String SCHEME = "module";
    public static final String ROOT = "/";
    public static final String SEPARATOR = "/";

    private static final Logger logger = LoggerFactory.getLogger(ModuleFileSystemProvider.class);

    private final ModuleRegistry registry;
    private ConcurrentMap<URI, ModuleFileSystem> moduleFileSystems = new ConcurrentHashMap<>();
    private final ReentrantLock creationLock = new ReentrantLock();

    public ModuleFileSystemProvider(Module module) {
        registry = new TableModuleRegistry();
        registry.add(module);
    }

    public ModuleFileSystemProvider(ModuleRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String getScheme() {
        return SCHEME;
    }

    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        Module module = uriToModule(uri);
        if (module == null) {
            throw new IllegalArgumentException("Unable to resolve " + uri.toString());
        }
        return newFileSystem(module);
    }

    @Override
    public FileSystem getFileSystem(URI uri) {
        if (moduleFileSystems.containsKey(uri)) {
            return moduleFileSystems.get(uri);
        }
        throw new FileSystemNotFoundException("Filesystem " + uri + " does not exist or has not been loaded");
    }

    /**
     * Creates a new file system for the given module.
     *
     * @param module The module to create a file system for
     * @return A new filesystem for the module
     * @throws IOException
     * @throws java.nio.file.FileSystemAlreadyExistsException If a filesystem already exists for the given module
     */
    public ModuleFileSystem newFileSystem(Module module) throws IOException {
        URI uri;
        try {
            uri = moduleToUri(module);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Could not generate URI for module '" + module.toString() + "'", e);
        }
        creationLock.lock();
        try {
            if (moduleFileSystems.containsKey(uri)) {
                throw new FileSystemAlreadyExistsException("Already created filesystem for '" + uri + "'");
            }
            ModuleFileSystem newFileSystem = new ModuleFileSystem(this, module);
            moduleFileSystems.put(uri, newFileSystem);
            return newFileSystem;
        } finally {
            creationLock.unlock();
        }
    }

    private URI moduleToUri(Module module) throws URISyntaxException {
        return new URI(String.format("%s://%s:%s", getScheme(), module.getId().toString(), module.getVersion().toString()));
    }

    private Module uriToModule(URI uri) {
        if (uri != null && uri.getScheme().equalsIgnoreCase(this.getScheme())) {
            String[] authorityParts = uri.getAuthority().split(":", 2);
            if (authorityParts.length != 2) {
                throw new IllegalArgumentException("Malformed uri: '" + uri.toString() + "'");
            }
            Name moduleId = new Name(authorityParts[0]);
            Version version = new Version(authorityParts[1]);
            return registry.getModule(moduleId, version);
        } else {
            throw new IllegalArgumentException("URI scheme is not '" + this.getScheme() + "'");
        }
    }

    @Override
    public Path getPath(URI uri) {
        Module module = uriToModule(uri);
        if (module == null) {
            throw new IllegalArgumentException("Failed to resolve uri '" + uri + "', module not found");
        }
        try {
            ModuleFileSystem fileSystem = newFileSystem(module);
            return fileSystem.getPath(uri.getPath());
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to resolve uri '" + uri + "'", e);
        }
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        Path underlying = getUnderlyingPath(path);
        return underlying.getFileSystem().provider().newByteChannel(underlying, options, attrs);
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        ModulePath modulePath = getModulePath(dir);
        if (!Files.exists(modulePath) || !Files.isDirectory(modulePath)) {
            throw new NotDirectoryException(dir.toString());
        }
        return new ModuleDirectoryStream(modulePath, filter);
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(Path path) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSameFile(Path path, Path path2) throws IOException {
        if (Objects.equals(path, path2)) {
            return true;
        }
        if (path == null || path2 == null) {
            return false;
        }
        Path resolvedPath1 = getUnderlyingPath(path);
        Path resolvedPath2 = getUnderlyingPath(path2);
        return resolvedPath1.getFileSystem().equals(resolvedPath2.getFileSystem()) && resolvedPath1.getFileSystem().provider().isSameFile(resolvedPath1, resolvedPath2);
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        Path underlying = getUnderlyingPath(path);
        return underlying.getFileSystem().provider().isHidden(underlying);
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        Path underlying = getUnderlyingPath(path);
        underlying.getFileSystem().provider().checkAccess(underlying, modes);
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        try {
            Path underlying = getUnderlyingPath(path);
            return underlying.getFileSystem().provider().getFileAttributeView(underlying, type, options);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
        Path underlying = getUnderlyingPath(path);
        return underlying.getFileSystem().provider().readAttributes(underlying, type, options);
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        Path underlying = getUnderlyingPath(path);
        return underlying.getFileSystem().provider().readAttributes(underlying, attributes, options);
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Called when a module file system is closed, removes it from the provider. It can be recreated if desired.
     * @param moduleFileSystem The filesystem to remove
     */
    void removeFileSystem(ModuleFileSystem moduleFileSystem) {
        creationLock.lock();
        try {
            moduleFileSystems.remove(moduleToUri(moduleFileSystem.getModule()));
        } catch (URISyntaxException e) {
            logger.error("Failed to close module, unable to resolve uri", e);
        } finally {
            creationLock.unlock();
        }
    }

    private ModulePath getModulePath(Path path) {
        if (path instanceof ModulePath) {
            return (ModulePath) path;
        }
        throw new ProviderMismatchException();
    }

    private Path getUnderlyingPath(Path path) throws IOException {
        ModulePath modulePath = getModulePath(path);
        Path underlying = modulePath.getUnderlyingPath();
        if (underlying != null) {
            return underlying;
        } else {
            throw new IOException("Path does not exist: " + path.toString());
        }
    }

    /**
     * A DirectoryStream that streams across all the locations composing a module
     */
    private static class ModuleDirectoryStream implements DirectoryStream<Path> {
        private Set<Path> contents = Sets.newLinkedHashSet();

        public ModuleDirectoryStream(ModulePath modulePath, Filter<? super Path> filter) {
            Module module = modulePath.getFileSystem().getModule();
            ModulePath normalisedPath = modulePath.toAbsolutePath().normalize();

            for (Path location : module.getLocations()) {
                try {
                    if (Files.isDirectory(location)) {
                        Path actualLocation = ModulePath.applyModulePathToActual(location, normalisedPath);
                        if (Files.exists(actualLocation)) {
                            Filter<? super Path> wrappedFilter = new ModulePathWrappedFilter(filter, actualLocation, modulePath.getFileSystem());
                            try (DirectoryStream<Path> stream = actualLocation.getFileSystem().provider().newDirectoryStream(actualLocation, wrappedFilter)) {
                                for (Path content : stream) {
                                    contents.add(ModulePath.convertFromActualToModulePath(modulePath.getFileSystem(), location, content));
                                }
                            }
                        }
                    } else if (Files.isRegularFile(location)) {
                        FileSystem moduleArchive = modulePath.getFileSystem().getContainedFileSystem(location);
                        for (Path archiveRoot : moduleArchive.getRootDirectories()) {
                            Path actualLocation = ModulePath.applyModulePathToActual(archiveRoot, normalisedPath);
                            if (Files.exists(actualLocation)) {
                                Filter<? super Path> wrappedFilter = new ModulePathWrappedFilter(filter, actualLocation, modulePath.getFileSystem());
                                try (DirectoryStream<Path> stream = actualLocation.getFileSystem().provider().newDirectoryStream(actualLocation, wrappedFilter)) {
                                    for (Path content : stream) {
                                        contents.add(ModulePath.convertFromActualToModulePath(modulePath.getFileSystem(), archiveRoot, content));
                                    }
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    logger.error("Failed to access module location " + location, e);
                }
            }
        }

        @Override
        public Iterator<Path> iterator() {
            return contents.iterator();
        }

        @Override
        public void close() throws IOException {

        }
    }

    /**
     * Wraps a filter provided so that it receives ModulePaths rather than Paths belonging to different file systems.
     */
    private static class ModulePathWrappedFilter implements DirectoryStream.Filter<Path> {

        private DirectoryStream.Filter<? super Path> innerFilter;
        private Path basePath;
        private ModuleFileSystem moduleFileSystem;

        public ModulePathWrappedFilter(DirectoryStream.Filter<? super Path> innerFilter, Path basePath, ModuleFileSystem moduleFileSystem) {
            this.innerFilter = innerFilter;
            this.basePath = basePath;
            this.moduleFileSystem = moduleFileSystem;
        }

        @Override
        public boolean accept(Path entry) throws IOException {
            return innerFilter.accept(ModulePath.convertFromActualToModulePath(moduleFileSystem, basePath, entry));
        }
    }
}
