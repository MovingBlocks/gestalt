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

package org.terasology.util.io;

import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

/**
 * Uitlity helper class for scanning Paths for files of interest.
 * @author Immortius
 */
public final class FileScanning {

    private static final PathMatcher ACCEPT_ALL_PATH_MATCHER = new AcceptAllPathMatcher();

    private FileScanning() {
    }

    /**
     * @return A trivial PathMatcher that accepts all paths
     */
    public static PathMatcher acceptAll() {
        return ACCEPT_ALL_PATH_MATCHER;
    }

    /**
     * Scans a path, recursing all paths matching the scanFilter and returning all files matching the fileFilter
     * @param rootPath The path to scan
     * @param scanFilter A PathMatcher indicating which subpaths to scan
     * @param fileFilter A PathMatcher indicating which files to return
     * @return A list of matching files within the path tree
     * @throws IOException If there is a problem walking the file tree
     */
    public static List<Path> findFilesInPath(Path rootPath, PathMatcher scanFilter, PathMatcher fileFilter) throws IOException {
        final ImmutableList.Builder<Path> resultBuilder = ImmutableList.builder();
        Files.walkFileTree(rootPath, new FileScanning.FilteredFileVisitor(scanFilter, fileFilter) {

            @Override
            protected void onMatch(Path file) {
                resultBuilder.add(file);
            }
        });
        return resultBuilder.build();
    }

    /**
     * A file visitor that filters by a pair of PathMatchers - one to determine which directories to visit, another determining which files
     * 'match', triggering the {@link #onMatch(Path) onMatch} method.
     */
    public abstract static class FilteredFileVisitor extends SimpleFileVisitor<Path> {

        private PathMatcher directoryFilter;
        private PathMatcher fileFilter;

        /**
         * @param directoryFilter A filter determining which directories to visit
         * @param fileFilter A filter determining which files match
         */
        public FilteredFileVisitor(PathMatcher directoryFilter, PathMatcher fileFilter) {
            this.directoryFilter = directoryFilter;
            this.fileFilter = fileFilter;
        }

        /**
         * This method is called for each matching file
         * @param file A matching file
         */
        protected abstract void onMatch(Path file);

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            if (directoryFilter.matches(dir)) {
                return FileVisitResult.CONTINUE;
            }
            return FileVisitResult.SKIP_SUBTREE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (fileFilter.matches(file)) {
                onMatch(file);
            }
            return FileVisitResult.CONTINUE;
        }
    }

    /**
     * Trivial path matcher that matches all paths
     */
    private static class AcceptAllPathMatcher implements PathMatcher {

        @Override
        public boolean matches(Path path) {
            return true;
        }
    }
}
