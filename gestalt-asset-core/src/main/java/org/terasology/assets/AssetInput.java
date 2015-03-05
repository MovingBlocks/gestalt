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

package org.terasology.assets;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Objects;

/**
 * @author Immortius
 */
public class AssetInput {

    private final Path path;

    public AssetInput(Path path) {
        this.path = path;
    }

    public String getFilename() {
        return path.getFileName().toString();
    }

    public String getFileExtension() {
        String filename = getFilename();
        if (filename.contains(".")) {
            return filename.substring(filename.lastIndexOf(".") + 1);
        }
        return "";
    }

    public BufferedInputStream openStream() throws IOException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<BufferedInputStream>() {
                @Override
                public BufferedInputStream run() throws IOException {
                    return new BufferedInputStream(Files.newInputStream(path));
                }
            });
        } catch (PrivilegedActionException e) {
            throw new IOException("Failed to open stream for '" + path + "'", e);
        }
    }

    @Override
    public String toString() {
        return path.toUri().toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof AssetInput) {
            AssetInput other = (AssetInput) obj;
            return Objects.equals(other.path, path);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }
}
