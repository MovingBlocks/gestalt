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

package org.terasology.module;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * A module that exists on the file system, but outside the classpath.
 * @author Immortius
 */
public class PathModule extends BaseModule {
    private static final Logger logger = LoggerFactory.getLogger(PathModule.class);

    private Path path;

    public PathModule(Path path, ModuleMetadata metadata) {
        super(Arrays.asList(path), metadata);
        this.path = path;
    }

    @Override
    public boolean isOnClasspath() {
        return false;
    }

    @Override
    public boolean isDataAvailable() {
        return Files.isRegularFile(path);
    }

    @Override
    public InputStream getData() throws IOException {
        return new BufferedInputStream(Files.newInputStream(path));
    }

    @Override
    public long size() {
        try {
            return Files.size(path);
        } catch (IOException e) {
            logger.warn("Failed to read file size for '{}'", path);
            return 0;
        }
    }
}
