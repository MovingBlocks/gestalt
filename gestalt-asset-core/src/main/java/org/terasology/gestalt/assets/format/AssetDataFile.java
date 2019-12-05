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

package org.terasology.gestalt.assets.format;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;

import net.jcip.annotations.Immutable;

import org.terasology.gestalt.module.resources.FileReference;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
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

    private final FileReference file;

    /**
     * @param file The AssetDataFile
     */
    public AssetDataFile(FileReference file) {
        Preconditions.checkNotNull(file);
        this.file = file;
    }

    /**
     * @return The path to the file (excluding file name) relative to the module
     */
    public List<String> getPath() {
        return file.getPath();
    }

    /**
     * @return The name of the file (including extension)
     */
    public String getFilename() {
        return file.getName();
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
    public InputStream openStream() throws IOException {
        try {
            return AccessController.doPrivileged((PrivilegedExceptionAction<InputStream>) file::open);
        } catch (PrivilegedActionException e) {
            throw new IOException("Failed to open stream for '" + file + "'", e);
        }
    }

    /**
     * Opens a reader to read the file. It is up to the reader's user to close it after use.
     * It is assumed the file is UTF-8 encoded
     *
     * @return A new buffered reader.
     * @throws IOException If there was an error opening the file
     */
    public BufferedReader openReader() throws IOException {
        return openReader(Charsets.UTF_8);
    }

    /**
     * Opens a reader to read the file. It is up to the reader's user to close it after use.
     *
     * @param charset The character set to interpret the file with
     * @return A new buffered reader.
     * @throws IOException If there was an error opening the file
     */
    public BufferedReader openReader(Charset charset) throws IOException {
        return new BufferedReader(new InputStreamReader(openStream(), charset));
    }


    @Override
    public String toString() {
        return file.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof AssetDataFile) {
            AssetDataFile other = (AssetDataFile) obj;
            return Objects.equals(other.file, file);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(file);
    }
}
