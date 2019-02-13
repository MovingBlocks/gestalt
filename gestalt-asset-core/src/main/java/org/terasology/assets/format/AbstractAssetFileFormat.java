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

package org.terasology.assets.format;

import org.terasology.assets.AssetData;
import org.terasology.assets.exceptions.InvalidAssetFilenameException;
import org.terasology.module.resources.ModuleFile;
import org.terasology.naming.Name;
import org.terasology.util.Varargs;
import org.terasology.util.io.FileExtensionPathMatcher;

import java.nio.file.PathMatcher;
import java.util.function.Predicate;

/**
 * A base implementation of {@link org.terasology.assets.format.AssetFileFormat AssetFileFormat} that will handle files with specified file extensions.
 * The name of the corresponding asset is assumed to be the non-extension part of the file name.
 * @author Immortius
 */
public abstract class AbstractAssetFileFormat<T extends AssetData> implements AssetFileFormat<T> {

    private Predicate<ModuleFile> fileMatcher;

    /**
     * @param fileExtension A file extension that this file format will handle
     * @param fileExtensions Additional file extensions that this file format will handle
     */
    public AbstractAssetFileFormat(String fileExtension, String... fileExtensions) {
        this.fileMatcher = FileUtil.createFileExtensionPredicate(Varargs.combineToList(fileExtension, fileExtensions));
    }

    public AbstractAssetFileFormat(Predicate<ModuleFile> fileMatcher) {
        this.fileMatcher = fileMatcher;
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
    public Predicate<ModuleFile> getFileMatcher() {
        return fileMatcher;
    }
}
