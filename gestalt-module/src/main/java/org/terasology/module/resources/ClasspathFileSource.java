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

package org.terasology.module.resources;

import android.support.annotation.NonNull;

import com.google.common.base.Joiner;

import org.reflections.Reflections;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
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

    private final Reflections manifest;
    private final String basePath;
    private final ClassLoader classLoader;

    /**
     * @param resourceManifest A reflections manifest indicating what files are available on the classpath
     */
    public ClasspathFileSource(Reflections resourceManifest) {
        this(resourceManifest, CLASS_PATH_SEPARATOR, ClassLoader.getSystemClassLoader());
    }

    /**
     * @param resourceManifest A reflections manifest indicating what files are available on the classpath
     * @param basePath         A subpath in the classpath to expose resources from
     */
    public ClasspathFileSource(Reflections resourceManifest, String basePath) {
        this(resourceManifest, basePath, ClassLoader.getSystemClassLoader());
    }

    /**
     * @param resourceManifest A reflections manifest indicating what files are available on the classpath
     * @param basePath         A subpath in the classpath to expose resources from
     * @param classLoader      The classloader to use to access resources
     */
    public ClasspathFileSource(Reflections resourceManifest, String basePath, ClassLoader classLoader) {
        this.manifest = resourceManifest;
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
    public Optional<ModuleFile> getFile(List<String> filepath) {
        String path = basePath + CLASS_PATH_JOINER.join(filepath);
        return manifest.getResources(x -> true).stream().filter(path::equals).<ModuleFile>map(x -> new ClasspathSourceFile(x, extractSubpath(x), classLoader)).findAny();
    }

    @Override
    public Collection<ModuleFile> getFilesInPath(boolean recursive, List<String> path) {
        String fullPath = buildPathString(path);
        return manifest.getResources(x -> true).stream().filter(x -> x.startsWith(fullPath) && (recursive || !x.substring(fullPath.length()).contains(CLASS_PATH_SEPARATOR))).map(x -> new ClasspathSourceFile(x, extractSubpath(x), classLoader)).collect(Collectors.toList());
    }

    @Override
    public Set<String> getSubpaths(List<String> path) {
        String fullPath = buildPathString(path);
        return manifest.getResources(x -> true).stream().filter(x -> x.startsWith(fullPath) && x.substring(fullPath.length()).contains(CLASS_PATH_SEPARATOR)).map(x -> {
            String subpath = x.substring(fullPath.length());
            return subpath.substring(0, subpath.indexOf(CLASS_PATH_SEPARATOR));
        }).collect(Collectors.toSet());
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
    public Iterator<ModuleFile> iterator() {
        return getFiles().iterator();
    }

    private static class ClasspathSourceFile implements ModuleFile {

        private final String path;
        private final String subpath;
        private final ClassLoader classLoader;

        ClasspathSourceFile(String resourcePath, String subpath, ClassLoader classLoader) {
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
            if (o instanceof ClasspathSourceFile) {
                ClasspathSourceFile other = (ClasspathSourceFile) o;
                return Objects.equals(other.path, path) && Objects.equals(other.classLoader, classLoader);
            }
            return false;
        }
    }

}
