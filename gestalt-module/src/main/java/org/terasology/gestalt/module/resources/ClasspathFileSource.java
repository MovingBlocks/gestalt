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

package org.terasology.gestalt.module.resources;

import android.support.annotation.NonNull;
import com.google.common.base.Joiner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ModuleFileSource that exposes the content from the classpath, using a Reflections manifest to
 * determine what files are available.
 */
public class ClasspathFileSource implements ModuleFileSource {

    private static final String CLASS_PATH_SEPARATOR = "/";
    private static final Joiner CLASS_PATH_JOINER = Joiner.on(CLASS_PATH_SEPARATOR);

    private final String basePath;
    private final ClassLoader classLoader;


    public ClasspathFileSource() {
        this(CLASS_PATH_SEPARATOR, ClassLoader.getSystemClassLoader());
    }

    /**
     * @param basePath A subpath in the classpath to expose resources from
     */
    public ClasspathFileSource(String basePath) {
        this(basePath, ClassLoader.getSystemClassLoader());
    }

    /**
     * @param basePath    A subpath in the classpath to expose resources from
     * @param classLoader The classloader to use to access resources
     */
    public ClasspathFileSource(String basePath, ClassLoader classLoader) {
        this.classLoader = classLoader;
        String path = basePath;
        if (!path.isEmpty() && !path.endsWith(CLASS_PATH_SEPARATOR)) {
            path = path + CLASS_PATH_SEPARATOR;
        }
        if (path.startsWith(CLASS_PATH_SEPARATOR)) {
            path = path.substring(1);
        }
        this.basePath = path;

    }

    @Override
    public Optional<FileReference> getFile(List<String> filepath) {
        if (filepath.stream().anyMatch(s -> s.equals(".."))) {
            return Optional.empty();
        }
        String fullpath = buildPathString(filepath);
        if (classLoader.getResource(fullpath) != null) {
            return Optional.of(new ClasspathSourceFileReference(fullpath.substring(0, fullpath.length() - 1), extractSubpath(fullpath), classLoader));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Collection<FileReference> getFilesInPath(boolean recursive, List<String> path) {
        if (path.stream().anyMatch(s -> s.equals(".."))) {
            return Collections.emptySet();
        }
        String fullPath = buildPathString(path);
        URL resource = classLoader.getResource(fullPath);
        if (resource == null) {
            return Collections.emptyList();
        }

        try {
            //TODO replaces with old  way. (android support)
            Path root = Paths.get(resource.toURI());
            return Files.walk(root, recursive ? 99 : 1)
                    .filter(Files::isRegularFile)
                    .filter(f -> !f.toFile().getName().endsWith(".class"))
                    .map(p -> new ClasspathSourceFileReference(p.getFileName().toString(), extractSubpath(root.getParent().relativize(p).toString()), classLoader))
                    .collect(Collectors.toList());
        } catch (IOException | URISyntaxException e) {
            return Collections.emptySet();
        }
    }

    @Override
    public Set<String> getSubpaths(List<String> path) {
        if (path.stream().anyMatch(s -> s.equals(".."))) {
            return Collections.emptySet();
        }
        String fullPath = buildPathString(path);
        URL resource = classLoader.getResource(fullPath);
        if (resource == null) {
            return Collections.emptySet();
        }

        try {
            //TODO replaces with old  way. (android support)
            Path root = Paths.get(resource.toURI());
            return Files.walk(root, 1)
                    .filter(Files::isDirectory)
                    .filter(dir -> !dir.equals(root))
                    .map(Path::toFile)
                    .map(File::getName)
                    .collect(Collectors.toSet());
        } catch (IOException | URISyntaxException e) {
            return Collections.emptySet();
        }
    }

    private String buildPathString(List<String> path) {
        String fullPath;
        if (path.isEmpty() || (path.size() == 1 && path.get(0).isEmpty())) {
            fullPath = basePath;
        } else {
            fullPath = basePath + CLASS_PATH_JOINER.join(path) + CLASS_PATH_SEPARATOR;
        }
        return fullPath;
    }

    private String extractSubpath(String path) {
        return path.substring(basePath.length());
    }

    @NonNull
    @Override
    public Iterator<FileReference> iterator() {
        return getFiles().iterator();
    }

    private static class ClasspathSourceFileReference implements FileReference {

        private final String path;
        private final String subpath;
        private final ClassLoader classLoader;

        ClasspathSourceFileReference(String resourcePath, String subpath, ClassLoader classLoader) {
            this.path = resourcePath;
            this.subpath = subpath;
            this.classLoader = classLoader;
        }

        @Override
        public String getName() {
            return subpath.substring(subpath.lastIndexOf(CLASS_PATH_SEPARATOR) + 1);
        }

        @Override
        public List<String> getPath() {
            List<String> parts = Arrays.asList(subpath.split(CLASS_PATH_SEPARATOR));
            return parts.subList(0, parts.size() - 1);
        }

        @Override
        public InputStream open() {
            return classLoader.getResourceAsStream(path);
        }

        @Override
        public String toString() {
            return getName();
        }

        @Override
        public int hashCode() {
            return Objects.hash(path, classLoader);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (o instanceof ClasspathSourceFileReference) {
                ClasspathSourceFileReference other = (ClasspathSourceFileReference) o;
                return Objects.equals(other.path, path) && Objects.equals(other.classLoader, classLoader);
            }
            return false;
        }
    }

}
