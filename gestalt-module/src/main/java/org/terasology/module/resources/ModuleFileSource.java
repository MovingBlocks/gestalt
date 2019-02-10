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

import android.support.annotation.RequiresApi;

import org.terasology.util.Varargs;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * ModuleFileSource provides an interface for all providers of files (resources) that are part
 * of the content of a Module. This includes file discovery and reading.
 * <p>
 * As a number of different mechanisms can be used to provide files, ModuleFileSource provides a
 * simplified view, where:
 * <ul>
 * <li>A file path is a list of strings, each representing a folder or step in the path</li>
 * <li>It is possible to discover what paths are within a path</li>
 * <li>It is possible to discover what files are within a path, optionally recursively</li>
 * <li>A file can be streamed</li>
 * </ul>
 * <p>
 * Paths are represented as a {@link List} of String path elements, where each element is a
 * directory or file. For example, if the ModuleFileSource is reading from a directory
 * "content/stuff/blurg", this would be represented as the path ["content", "stuff", "blurg"]
 */
public interface ModuleFileSource extends Iterable<ModuleFile> {

    /**
     * Obtain the handle to a specific file. The file path should be provided as one or more
     * string elements that together compose the path
     *
     * @param path     The path to the file
     * @param morePath More path to the file
     * @return The requested file, or {@link Optional#empty()} if it doesn't exist
     */
    default Optional<ModuleFile> getFile(String path, String... morePath) {
        return getFile(Varargs.combineToList(path, morePath));
    }

    /**
     * Obtain the handle to a specific file
     *
     * @param filepath The path to the file. Should not be empty
     * @return The requested file, or {@link Optional#empty()} if it doesn't exist
     */
    Optional<ModuleFile> getFile(List<String> filepath);

    /**
     * @return A collection of all files provided by this ModuleFileSource
     */
    default Collection<ModuleFile> getFiles() {
        return getFilesInPath(true);
    }

    /**
     * Finds all files within a path
     *
     * @param recursive Whether to recurse through subpaths
     * @param path      The path to search
     * @return A collection of handles to all files in the give path
     */
    default Collection<ModuleFile> getFilesInPath(boolean recursive, String... path) {
        return getFilesInPath(recursive, Arrays.asList(path));
    }

    /**
     * Finds all files within a path
     *
     * @param recursive Whether to recurse through subpaths
     * @param path      The path to search
     * @return A collection of handles to all files in the give path
     */
    Collection<ModuleFile> getFilesInPath(boolean recursive, List<String> path);

    /**
     * Finds all subpaths in the given path
     *
     * @param fromPath The path to search
     * @return A list of the immediate subpaths in the given path
     */
    default Set<String> getSubpaths(String... fromPath) {
        return getSubpaths(Arrays.asList(fromPath));
    }

    /**
     * Finds all subpaths in the given path
     *
     * @param fromPath The path to search
     * @return A list of the immediate subpaths in the given path
     */
    Set<String> getSubpaths(List<String> fromPath);

    /**
     *
     * @return A list of all the root paths of this file source, that
     */
    @RequiresApi(26)
    default List<Path> getRootPaths() {
        return Collections.emptyList();
    }

}
