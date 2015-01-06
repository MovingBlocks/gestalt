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

package org.terasology.module.filesystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.module.Module;
import org.terasology.module.ModuleRegistry;
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
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Immortius
 */
public class ModuleFileSystemProvider extends FileSystemProvider {

    public static final String SCHEME = "module";
    private static final Logger logger = LoggerFactory.getLogger(ModuleFileSystemProvider.class);

    private ModuleRegistry registry;
    private ConcurrentMap<URI, ModuleFileSystem> moduleFileSystems = new ConcurrentHashMap<>();
    private final ReentrantLock creationLock = new ReentrantLock();

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
     * @param module
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
        return null;
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        return null;
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {

    }

    @Override
    public void delete(Path path) throws IOException {

    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {

    }

    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {

    }

    @Override
    public boolean isSameFile(Path path, Path path2) throws IOException {
        return false;
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        return false;
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        return null;
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {

    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        return null;
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
        return null;
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        return null;
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {

    }

    public void removeFileSystem(ModuleFileSystem moduleFileSystem) {
        creationLock.lock();
        try {
            moduleFileSystems.remove(moduleToUri(moduleFileSystem.getModule()));
        } catch (URISyntaxException e) {
            logger.error("Failed to close module, unable to resolve uri", e);
        } finally {
            creationLock.unlock();
        }
    }
}
