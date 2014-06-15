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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.module.exceptions.InvalidModulePathException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * A module that exists on the file system, but outside the classpath.
 *
 * @author Immortius
 */
public class PathModule extends BaseModule {
    private static final Logger logger = LoggerFactory.getLogger(PathModule.class);

    private final Path path;
    private final Collection<URL> classpaths;

    /**
     * Creates a path module with no code
     *
     * @param path     The root path of the module
     * @param codePath The relative location of code for the module (relative to root path)
     * @param metadata
     */
    public PathModule(Path path, Path codePath, ModuleMetadata metadata) {
        super(Arrays.asList(path), metadata);
        Preconditions.checkArgument(Files.isDirectory(path));
        this.path = path;

        Path fullCodePath = path.resolve(codePath);
        if (Files.isDirectory(fullCodePath)) {
            try {
                classpaths = ImmutableList.of(fullCodePath.toUri().toURL());
            } catch (MalformedURLException e) {
                throw new InvalidModulePathException("Unable to convert path to URL: " + fullCodePath, e);
            }
        } else {
            classpaths = Collections.emptyList();
        }
    }

    @Override
    public Collection<URL> getClasspaths() {
        return classpaths;
    }

    @Override
    public boolean isOnClasspath() {
        return false;
    }

    @Override
    public boolean isCodeModule() {
        return !classpaths.isEmpty();
    }
}
