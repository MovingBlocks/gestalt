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

import org.terasology.assets.AssetData;
import org.terasology.assets.exceptions.InvalidAssetFilenameException;
import org.terasology.naming.Name;
import org.terasology.util.Varargs;
import org.terasology.util.io.FileExtensionPathMatcher;

import java.nio.file.PathMatcher;

/**
 * A base implementation of {@link org.terasology.assets.format.AssetAlterationFileFormat AssetAlterationFileFormat} that will handle files with specified file extensions.
 * The name of the corresponding asset is assumed to be the non-extension part of the file name.
 *
 * @author Immortius
 */
public abstract class AbstractAssetAlterationFileFormat<T extends AssetData> implements AssetAlterationFileFormat<T> {

    private FileExtensionPathMatcher fileMatcher;

    /**
     * @param fileExtension A file extension that this file format will handle
     * @param fileExtensions Additional file extensions that this file format will handle
     */
    public AbstractAssetAlterationFileFormat(String fileExtension, String... fileExtensions) {
        this.fileMatcher = new FileExtensionPathMatcher(Varargs.combineToSet(fileExtension, fileExtensions));
    }

    @Override
    public Name getAssetName(String filename) throws InvalidAssetFilenameException {
        int extensionStart = filename.lastIndexOf('.');
        if (extensionStart != -1) {
            return new Name(filename.substring(0, extensionStart));
        }
        return new Name(filename);
    }

    @Override
    public PathMatcher getFileMatcher() {
        return fileMatcher;
    }
}
