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

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.terasology.module.Module;
import org.terasology.naming.Name;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The Path implementation used by ModuleFileSystem.
 *
 * @author Immortius
 */
class ModulePath implements Path {

    private static final String SAME_DIR_INDICATOR = ".";
    private static final String PREVIOUS_DIR_INDICATOR = "..";

    private final String path;
    private final ModuleFileSystem fileSystem;

    ModulePath(String path, ModuleFileSystem fileSystem) {
        Preconditions.checkNotNull(path);
        Preconditions.checkNotNull(fileSystem);
        this.path = path;
        this.fileSystem = fileSystem;
    }

    @Override
    public ModuleFileSystem getFileSystem() {
        return fileSystem;
    }

    @Override
    public boolean isAbsolute() {
        return !path.isEmpty() && path.startsWith(fileSystem.getSeparator());
    }

    @Override
    public ModulePath getRoot() {
        if (isAbsolute()) {
            return new ModulePath(fileSystem.getSeparator(), fileSystem);
        }
        return null;
    }

    @Override
    public ModulePath getFileName() {
        if (path.isEmpty()) {
            return this;
        }
        if (path.equals(fileSystem.getSeparator())) {
            return null;
        }
        List<String> parts = getPathPartsExcludingRoot();
        return new ModulePath(parts.get(parts.size() - 1), fileSystem);
    }

    @Override
    public ModulePath getParent() {
        List<String> parts = getPathPartsIncludingRoot();
        if (parts.size() <= 1) {
            return null;
        }
        return newPathFromParts(fileSystem, parts, 0, parts.size() - 1);
    }

    @Override
    public int getNameCount() {
        return getPathPartsExcludingRoot().size();
    }

    @Override
    public ModulePath getName(int index) {
        List<String> parts = getPathPartsExcludingRoot();
        if (index < 0 || index >= parts.size()) {
            throw new IllegalArgumentException("Index out of bounds, '" + index + "' not in range 0 <= index < " + parts.size());
        }
        return fileSystem.getPath(parts.get(index));
    }

    @Override
    public ModulePath subpath(int beginIndex, int endIndex) {
        List<String> parts = getPathPartsExcludingRoot();
        if (beginIndex < 0 || beginIndex >= parts.size()) {
            throw new IllegalArgumentException("beginIndex out of bounds: '" + beginIndex + "' not in range 0 <= beginIndex < " + parts.size());
        }
        if (endIndex <= beginIndex || endIndex > parts.size()) {
            throw new IllegalArgumentException("endIndex out of bounds: '" + endIndex + "' not in range " + beginIndex + " < endIndex <= " + parts.size());
        }
        return newPathFromParts(fileSystem, parts, beginIndex, endIndex);
    }

    @Override
    public boolean startsWith(Path other) {
        if (other instanceof ModulePath) {
            ModulePath otherModulePath = (ModulePath) other;
            if (!fileSystem.equals(otherModulePath.getFileSystem()) || isAbsolute() != other.isAbsolute()) {
                return false;
            }
            List<String> parts = getPathPartsExcludingRoot();
            List<String> otherParts = otherModulePath.getPathPartsExcludingRoot();
            if (otherParts.size() > parts.size()) {
                return false;
            }
            for (int i = 0; i < otherParts.size(); ++i) {
                if (!parts.get(i).equals(otherParts.get(i))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean startsWith(String other) {
        return startsWith(fileSystem.getPath(other));
    }

    @Override
    public boolean endsWith(Path other) {
        if (other instanceof ModulePath) {
            ModulePath otherModulePath = (ModulePath) other;
            if (!fileSystem.equals(otherModulePath.getFileSystem())) {
                return false;
            }
            List<String> parts = getPathPartsIncludingRoot();
            List<String> otherParts = otherModulePath.getPathPartsIncludingRoot();
            if (otherParts.size() > parts.size()) {
                return false;
            }
            int offset = parts.size() - otherParts.size();
            for (int i = 0; i < otherParts.size(); ++i) {
                if (!parts.get(i + offset).equals(otherParts.get(i))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean endsWith(String other) {
        return endsWith(fileSystem.getPath(other));
    }

    @Override
    public ModulePath normalize() {
        List<String> parts = getPathPartsIncludingRoot();
        List<String> normalisedParts = Lists.newArrayListWithCapacity(parts.size());
        for (String part : parts) {
            if (part.equals(PREVIOUS_DIR_INDICATOR)) {
                if (normalisedParts.isEmpty() || PREVIOUS_DIR_INDICATOR.equals(normalisedParts.get(normalisedParts.size() - 1))) {
                    normalisedParts.add(part);
                } else if (!fileSystem.getSeparator().equals(normalisedParts.get(normalisedParts.size() - 1))) {
                    normalisedParts.remove(normalisedParts.size() - 1);
                }
            } else if (!SAME_DIR_INDICATOR.equals(part)) {
                normalisedParts.add(part);
            }
        }
        if (normalisedParts.size() == parts.size()) {
            return this;
        } else if (normalisedParts.size() == 0) {
            return fileSystem.getPath("");
        }
        return newPathFromParts(fileSystem, normalisedParts);
    }


    @Override
    public ModulePath resolve(Path other) {
        if (other instanceof ModulePath && fileSystem.equals(other.getFileSystem())) {
            if (other.isAbsolute()) {
                return (ModulePath) other;
            }
            List<String> newParts = getPathPartsIncludingRoot();
            newParts.addAll(((ModulePath) other).getPathPartsExcludingRoot());
            return newPathFromParts(fileSystem, newParts);
        } else {
            throw new IllegalArgumentException("Cannot resolve path from another file system");
        }

    }

    @Override
    public ModulePath resolve(String other) {
        return resolve(fileSystem.getPath(other));
    }

    @Override
    public Path resolveSibling(Path other) {
        Path parentPath = getParent();
        if (parentPath == null || other.isAbsolute()) {
            return other;
        }
        return parentPath.resolve(other);
    }

    @Override
    public Path resolveSibling(String other) {
        return resolveSibling(fileSystem.getPath(other));
    }

    @Override
    public ModulePath relativize(Path other) {
        if (other instanceof ModulePath && other.getFileSystem().equals(fileSystem)) {
            ModulePath otherModulePath = (ModulePath) other;
            List<String> parts = normalize().getPathPartsExcludingRoot();
            List<String> otherParts = otherModulePath.normalize().getPathPartsExcludingRoot();
            int commonPathEnd = 0;
            while (commonPathEnd < parts.size() && commonPathEnd < otherParts.size() && parts.get(commonPathEnd).equals(otherParts.get(commonPathEnd))) {
                commonPathEnd++;
            }
            List<String> relativizedPath = Lists.newArrayList();
            for (int i = commonPathEnd; i < parts.size(); ++i) {
                relativizedPath.add("..");
            }
            if (commonPathEnd < otherParts.size()) {
                relativizedPath.addAll(otherParts.subList(commonPathEnd, otherParts.size()));
            }
            if (relativizedPath.isEmpty()) {
                relativizedPath.add("");
            }
            return newPathFromParts(fileSystem, relativizedPath);
        } else {
            throw new IllegalArgumentException("Cannot relativize path from another file system");
        }
    }

    @Override
    public URI toUri() {
        throw new UnsupportedOperationException("Module filesystem does not support URIs");
    }

    @Override
    public ModulePath toAbsolutePath() {
        if (isAbsolute()) {
            return this;
        } else {
            return fileSystem.getPath("/").resolve(this);
        }
    }

    @Override
    public Path toRealPath(LinkOption... options) throws IOException {
        Optional<Module> module = getModule();
        if (!module.isPresent()) {
            throw new IOException("Path does not exist: " + toString());
        }

        ModulePath normalisedPath = toAbsolutePath().normalize();

        for (Path location : module.get().getLocations()) {
            if (Files.isDirectory(location)) {
                Path actualLocation = applyModulePathToActual(location, normalisedPath);
                if (Files.exists(actualLocation)) {
                    return convertFromActualToModulePath(getFileSystem(), module.get().getId().toString(), location, actualLocation);
                }
            } else if (Files.isRegularFile(location)) {
                FileSystem moduleArchive = fileSystem.getRealFileSystem(location);
                for (Path archiveRoot : moduleArchive.getRootDirectories()) {
                    Path actualLocation = applyModulePathToActual(archiveRoot, normalisedPath);
                    if (Files.exists(actualLocation)) {
                        return convertFromActualToModulePath(getFileSystem(), module.get().getId().toString(), archiveRoot, actualLocation);
                    }
                }
            }
        }
        throw new IOException("Path does not exist: " + toString());
    }

    @Override
    public File toFile() {
        throw new UnsupportedOperationException("ModulePath does not support toFile()");
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException {
        if (watcher instanceof ModuleWatchService) {
            ModuleWatchService moduleWatcher = (ModuleWatchService) watcher;
            return moduleWatcher.register(this, events, modifiers);
        } else {
            throw new ProviderMismatchException("WatchService belongs to a different file system");
        }
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>... events) throws IOException {
        return register(watcher, events, new WatchEvent.Modifier[0]);
    }

    @Override
    public Iterator<Path> iterator() {
        return Collections2.transform(getPathPartsExcludingRoot(), new Function<String, Path>() {
            @Nullable
            @Override
            public Path apply(String input) {
                return fileSystem.getPath(input);
            }
        }).iterator();
    }

    @Override
    public int compareTo(Path other) {
        if (!(other instanceof ModulePath) || !((ModulePath) other).fileSystem.equals(fileSystem)) {
            throw new IllegalArgumentException("Cannot compare with path from another file system");
        }
        ModulePath otherModulePath = (ModulePath) other;
        return path.compareTo(otherModulePath.path);
    }

    @Override
    public String toString() {
        return path;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ModulePath) {
            ModulePath other = (ModulePath) obj;
            return other.fileSystem.equals(fileSystem) && this.compareTo(other) == 0;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileSystem, path.toLowerCase(Locale.ENGLISH));
    }

    Optional<Module> getModule() {
        ModulePath normalisedPath = toAbsolutePath().normalize();
        if (normalisedPath.getNameCount() > 0) {
            return Optional.ofNullable(fileSystem.getEnvironment().get(new Name(normalisedPath.getName(0).toString())));
        }
        return Optional.empty();
    }

    /**
     * @return A list of the name components of the path, including the root
     */
    List<String> getPathPartsIncludingRoot() {
        String[] parts = path.split(fileSystem.getSeparator(), 0);
        if (isAbsolute()) {
            List<String> result = Lists.newArrayListWithCapacity(parts.length);
            result.add("/");
            if (parts.length > 1) {
                result.addAll(Arrays.asList(parts).subList(1, parts.length));
            }
            return result;
        } else {
            return Lists.newArrayList(parts);
        }
    }

    /**
     * @return A list of the name components of the path, excluding the root
     */
    List<String> getPathPartsExcludingRoot() {
        String[] parts = path.split(fileSystem.getSeparator(), 0);
        if (isAbsolute()) {
            if (parts.length > 1) {
                return Arrays.asList(parts).subList(1, parts.length);
            } else {
                return Collections.emptyList();
            }
        } else {
            return Lists.newArrayList(parts);
        }
    }

    /**
     * Provides the underlying path (from one of the underlying filesystems/locations composing the module) for this path.
     * If this path doesn't exist, returns null.
     * If multiple paths exist, the first one discovered is returned.
     *
     * @return The real, underlying path for this path, or null if it doesn't exist.
     * @throws IOException
     */
    Optional<Path> getUnderlyingPath() throws IOException {
        Optional<Module> module = getModule();
        if (!module.isPresent()) {
            return Optional.empty();
        }

        ModulePath normalisedPath = toAbsolutePath().normalize();

        for (Path location : module.get().getLocations()) {
            if (Files.isDirectory(location)) {
                Path actualLocation = applyModulePathToActual(location, normalisedPath);
                if (Files.exists(actualLocation)) {
                    return Optional.of(actualLocation);
                }
            } else if (Files.isRegularFile(location)) {
                FileSystem moduleArchive = fileSystem.getRealFileSystem(location);
                for (Path archiveRoot : moduleArchive.getRootDirectories()) {
                    Path actualLocation = applyModulePathToActual(archiveRoot, normalisedPath);
                    if (Files.exists(actualLocation)) {
                        return Optional.of(actualLocation);
                    }
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Provides all the underlying paths (from one of the underlying filesystems/locations composing the module) for this path.
     *
     * @return A set of all the underlying paths from all locations composing the module
     * @throws IOException
     */
    Set<Path> getUnderlyingPaths() throws IOException {
        Set<Path> underlyingPaths = Sets.newLinkedHashSet();
        Optional<Module> module = getModule();
        if (!module.isPresent()) {
            return underlyingPaths;
        }

        ModulePath normalisedPath = toAbsolutePath().normalize();
        for (Path location : module.get().getLocations()) {
            if (Files.isDirectory(location)) {
                Path actualLocation = applyModulePathToActual(location, normalisedPath);
                if (Files.exists(actualLocation)) {
                    underlyingPaths.add(actualLocation);
                }
            } else if (Files.isRegularFile(location)) {
                FileSystem moduleArchive = fileSystem.getRealFileSystem(location);
                for (Path archiveRoot : moduleArchive.getRootDirectories()) {
                    Path actualLocation = applyModulePathToActual(archiveRoot, normalisedPath);
                    if (Files.exists(actualLocation)) {
                        underlyingPaths.add(actualLocation);
                    }
                }
            }
        }
        return underlyingPaths;
    }

    /**
     * Applies a modulePath on top of an external path - used to produce the underlying path.
     *
     * @param actualRoot The root path in a different filesystem
     * @param modulePath The module path to convert to a real path
     * @return The real path. This will be the actualRoot with the name parts of the modulePath appended.
     */
    static Path applyModulePathToActual(Path actualRoot, ModulePath modulePath) {
        Path result = actualRoot;
        List<String> parts = modulePath.getPathPartsExcludingRoot();
        if (parts.size() < 2) {
            return result;
        }
        for (String part : parts.subList(1, parts.size())) {
            result = result.resolve(part);
        }
        return result;
    }

    /**
     * @param fileSystem The fileSystem the produced path will belong to
     * @param parts      The parts to construct the path from
     * @return A new module path belonging to the given fileSystem and with the given name parts
     */
    static ModulePath newPathFromParts(ModuleFileSystem fileSystem, List<String> parts) {
        return newPathFromParts(fileSystem, parts, 0, parts.size());
    }

    /**
     * @param fileSystem The fileSystem the produced path will belong to
     * @param parts      The parts to construct the path from
     * @return A new module path belonging to the given fileSystem and with the given subset of name parts
     */
    static ModulePath newPathFromParts(ModuleFileSystem fileSystem, List<String> parts, int beginIndex, int endIndex) {
        String first = parts.get(beginIndex);
        String[] remainder = new String[endIndex - beginIndex - 1];
        for (int i = 0; i < remainder.length; ++i) {
            remainder[i] = parts.get(beginIndex + i + 1);
        }
        return fileSystem.getPath(first, remainder);
    }

    /**
     * @param fileSystem The module filesystem the new path should belong to
     * @param root       The root path the actual path is relative to
     * @param actualPath The path to convert
     * @return The module path equivalent of a given path
     * @throws IOException
     */
    static Path convertFromActualToModulePath(ModuleFileSystem fileSystem, String moduleName, Path root, Path actualPath) throws IOException {
        Path actualRealPath = actualPath.toRealPath();
        Path relative = root.toRealPath().relativize(actualRealPath);
        List<String> pathParts = Lists.newArrayListWithCapacity(relative.getNameCount() + 1);
        pathParts.add(ModuleFileSystemProvider.SEPARATOR);
        pathParts.add(moduleName);
        for (Path part : relative) {
            pathParts.add(part.toString());
        }
        return ModulePath.newPathFromParts(fileSystem, pathParts);
    }

}
