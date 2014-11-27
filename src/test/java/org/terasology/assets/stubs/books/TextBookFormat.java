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

package org.terasology.assets.stubs.books;

import org.terasology.assets.AssetFormat;
import org.terasology.assets.AssetInput;
import org.terasology.naming.ResourceUrn;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * @author Immortius
 */
public class TextBookFormat implements AssetFormat<BookData> {

    private ImmutableSet<String> fileExtensions =

    @Override
    public Set<String> getFileExtensions() {
         return
    }

    @Override
    public String getAssetName(String filename) {
        return null;
    }

    @Override
    public BookData load(ResourceUrn urn, List<AssetInput> inputs) throws IOException {
        return null;
    }
}
