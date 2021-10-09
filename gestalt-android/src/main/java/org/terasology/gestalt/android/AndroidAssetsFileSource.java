/*
 * Copyright 2021 The Terasology Foundation
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

package org.terasology.gestalt.android;

import android.content.res.AssetManager;
import android.support.annotation.NonNull;
import org.terasology.gestalt.module.resources.FileReference;
import org.terasology.gestalt.module.resources.ModuleFileSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A {@link ModuleFileSource} that can be used to access files stored in the "assets" directory of an Android APK.
 * It requires an {@link AssetManager} instance to use, which can be obtained using {@link android.content.Context#getAssets()}.
 */
public class AndroidAssetsFileSource implements ModuleFileSource {
    private final AssetManager assetManager;
    private final String moduleRoot;

    public AndroidAssetsFileSource(AssetManager assetManager, String moduleRoot) {
        this.assetManager = assetManager;
        this.moduleRoot = moduleRoot;
    }

    /**
     * Obtains the handle to a specific file
     *
     * @param filepath The path to the file. Should not be empty
     * @return The requested file, or {@link Optional#empty()} if it doesn't exist
     */
    @Override
    public Optional<FileReference> getFile(List<String> filepath) {
        try {
            String path = getPath(filepath);
            assetManager.open(path).close();
            return Optional.of(new AssetsFileReference(filepath.get(filepath.size() - 1), path));
        } catch (Exception ignore) {
            return Optional.empty();
        }
    }

    /**
     * Finds all files within a path
     *
     * @param recursive Whether to recurse through subpaths
     * @param path      The path to search
     * @return A collection of handles to all files in the give path
     */
    @Override
    public Collection<FileReference> getFilesInPath(boolean recursive, List<String> path) {
        List<FileReference> files = new ArrayList<>();
        String rootPath = getPath(path);

        try {
            for (String subPath : assetManager.list(moduleRoot + "/" + rootPath)) {
                files.add(new AssetsFileReference(subPath, rootPath));
                if (recursive) {
                    files.addAll(getFilesInPath(recursive,rootPath + "/" + subPath));
                }
            }
        } catch (IOException ignore) {
            return files;
        }

        return files;
    }

    /**
     * Finds all subpaths in the given path
     *
     * @param fromPath The path to search
     * @return A list of the immediate subpaths in the given path
     */
    @Override
    public Set<String> getSubpaths(List<String> fromPath) {
        Set<String> subPaths = new HashSet<>();
        String rootPath = getPath(fromPath);

        try {
            for (String subPath : assetManager.list(moduleRoot + "/" + rootPath)) {
                subPaths.add(subPath);
            }
        } catch (IOException ignore) {
            return subPaths;
        }

        return subPaths;
    }

    /**
     * Returns an iterator over elements of type {@code T}.
     *
     * @return an Iterator.
     */
    @NonNull
    @Override
    public Iterator<FileReference> iterator() {
        return getFilesInPath(true, Collections.singletonList("")).iterator();
    }

    private String getPath(List<String> parts) {
        StringBuilder pathBuilder = new StringBuilder();
        for (int partNo = 0; partNo < parts.size(); partNo++) {
            pathBuilder.append(parts.get(partNo));
            if (partNo != parts.size() - 1) {
                pathBuilder.append('/');
            }
        }
        return pathBuilder.toString();
    }

    private class AssetsFileReference implements FileReference {
        private final String name;
        private final String path;

        public AssetsFileReference(String name, String path) {
            this.name = name;
            this.path = path;
        }

        /**
         * @return The name of the file
         */
        @Override
        public String getName() {
            return name;
        }

        /**
         * @return The path to the file (within the file source), excluding the file name
         */
        @Override
        public List<String> getPath() {
            return Arrays.asList(path.split("/"));
        }

        /**
         * @return A new InputStream for reading the file. Closing the stream is the duty of the caller
         * @throws IOException If there is an exception opening the file
         */
        @Override
        public InputStream open() throws IOException {
            return assetManager.open(moduleRoot + "/" + path + "/" + name);
        }
    }
}
