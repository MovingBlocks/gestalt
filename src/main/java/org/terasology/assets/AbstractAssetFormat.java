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

import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 *
 * @author Immortius
 */
public abstract class AbstractAssetFormat<T extends AssetData> implements AssetFormat<T> {

    private ImmutableSet<String> fileExtensions;

    public AbstractAssetFormat(Set<String> fileExtensions) {
        this.fileExtensions = ImmutableSet.copyOf(fileExtensions);
    }

    @Override
    public String getAssetName(String filename) {
        int extensionStart = filename.lastIndexOf('.');
        if (extensionStart != -1) {
            return filename.substring(extensionStart);
        }
        return filename;
    }

    @Override
    public final Set<String> getFileExtensions() {
        return fileExtensions;
    }

}
