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

package org.terasology.gestalt.util.io;

import android.support.annotation.RequiresApi;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

import org.terasology.gestalt.util.Varargs;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Collection;
import java.util.Set;

/**
 * A PathMatcher that matches files ending with one of a set of file extensions.
 *
 * @author Immortius
 */
@RequiresApi(26)
public class FileExtensionPathMatcher implements PathMatcher {

    private final Set<String> extensions;

    /**
     * @param extension  One of the extensions that a file must have to match
     * @param extensions Additional extensions that files must have to match
     */
    public FileExtensionPathMatcher(String extension, String... extensions) {
        this(Varargs.combineToSet(extension, extensions));
    }

    /**
     * @param extensions The extensions that files must have to match. Must not be empty
     */
    public FileExtensionPathMatcher(Collection<String> extensions) {
        Preconditions.checkNotNull(extensions);
        Preconditions.checkArgument(!extensions.isEmpty(), "At least one extension must be provided");
        this.extensions = Sets.newHashSet(extensions);
    }

    @Override
    public boolean matches(Path path) {
        Path fileName = path.getFileName();
        if (fileName != null) {
            return extensions.contains(Files.getFileExtension(fileName.toString()));
        }
        return false;
    }
}
