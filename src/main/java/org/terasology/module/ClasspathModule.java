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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;

/**
 * A module that exists on the classpath. This means that it is always loaded and available, so should not be loaded in a secondary class loader.
 *
 * @author Immortius
 */
public class ClasspathModule extends BaseModule {

    public ClasspathModule(Collection<Path> paths, ModuleMetadata metadata) {
        super(paths, metadata);
    }

    @Override
    public boolean isOnClasspath() {
        return true;
    }

    @Override
    public boolean isDataAvailable() {
        return false;
    }

    @Override
    public InputStream getData() throws IOException {
        throw new UnsupportedOperationException("Built in modules cannot be streamed");
    }

    @Override
    public long size() {
        return 0;
    }
}
