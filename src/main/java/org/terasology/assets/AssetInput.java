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

package org.terasology.assets;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Immortius
 */
public class AssetInput implements Closeable {

    private final String filename;
    private final InputStream inputStream;

    public AssetInput(String filename, InputStream inputStream) {
        this.filename = filename;
        this.inputStream = inputStream;
    }

    public String getFilename() {
        return filename;
    }

    public String getFileExtension() {
        if (filename.contains(".")) {
            return filename.substring(filename.lastIndexOf(".") + 1);
        }
        return "";
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void close() throws IOException {
        inputStream.close();
    }

    @Override
    public String toString() {
        return filename;
    }
}
