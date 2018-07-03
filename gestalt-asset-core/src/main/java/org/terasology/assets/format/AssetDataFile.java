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

package org.terasology.assets.format;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import javax.annotation.concurrent.Immutable;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.List;
import java.util.Objects;

/**
 * An asset data file. Provides details on the file's name, extension and allows the file to be opened as a stream.
 * <p>
 * FileFormats are not given direct access to the Path or File, as asset types provided by modules may not have IO permissions.
 * </p>
 * <p>
 * Immutable.
 * </p>
 *
 * @author Immortius
 */
@Immutable
public class AssetDataFile {

    private final Path path;

    /**
     * @param path The path of the AssetDataFile
     */
    public AssetDataFile(Path path) {
        Preconditions.checkNotNull(path);
        this.path = path;
    }

    /**
     * @return The path to the file (excluding file name) relative to the module
     */
    public List<String> getPath() {
        List<String> result = Lists.newArrayListWithCapacity(path.getNameCount() - 1);
        for (int i = 0; i < path.getNameCount() - 1; ++i) {
            result.add(path.getName(i).toString());
        }
        return result;
    }

    /**
     * @return The name of the file (including extension)
     */
    public String getFilename() {
        Path filename = path.getFileName();
        if (filename != null) {
            return filename.toString();
        } else {
            throw new IllegalStateException("AssetDataFile has empty path");
        }
    }

    /**
     * @return The file extension.
     */
    public String getFileExtension() {
        String filename = getFilename();
        if (filename.contains(".")) {
            return filename.substring(filename.lastIndexOf(".") + 1);
        }
        return "";

    }

    /**
     * Opens a stream to read the file. It is up to the stream's user to close it after use.
     *
     * @return A new buffered input stream.
     * @throws IOException If there was an error opening the file
     */
    public BufferedInputStream openStream() throws IOException {
        try {
            return AccessController.doPrivileged((PrivilegedExceptionAction<BufferedInputStream>) () -> {
                Preconditions.checkState(Files.isRegularFile(path));
                return new BufferedInputStream(Files.newInputStream(path));
            });
        } catch (PrivilegedActionException e) {
            throw new IOException("Failed to open stream for '" + path + "'", e);
        }
    }

    @Override
    public String toString() {
        return path.toAbsolutePath().toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof AssetDataFile) {
            AssetDataFile other = (AssetDataFile) obj;
            return Objects.equals(other.path, path);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }
}
