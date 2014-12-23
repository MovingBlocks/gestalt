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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.CharStreams;
import org.terasology.assets.AbstractAssetFormat;
import org.terasology.assets.AssetInput;
import org.terasology.assets.exceptions.InvalidAssetFilenameException;
import org.terasology.naming.Name;
import org.terasology.naming.ResourceUrn;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

/**
 * @author Immortius
 */
public class TextBookFormat extends AbstractAssetFormat<BookData> {
    private static final String TXT_EXTENSION = "txt";
    private static final String BODY_TXT = "_body.txt";
    private static final String HEADER_TXT = "_header.txt";

    public TextBookFormat() {
        super(TXT_EXTENSION);
    }

    @Override
    public Name getAssetName(String filename) throws InvalidAssetFilenameException {
        if (filename.endsWith(BODY_TXT)) {
            return new Name(filename.substring(0, filename.length() - BODY_TXT.length()));
        } else if (filename.endsWith(HEADER_TXT)) {
            return new Name(filename.substring(0, filename.length() - HEADER_TXT.length()));
        }
        throw new InvalidAssetFilenameException("Missing header/body in filename");
    }

    @Override
    public BookData load(ResourceUrn urn, List<AssetInput> inputs) throws IOException {
        BookData data = new BookData();
        for (AssetInput input : inputs) {
            if (input.getFilename().endsWith(BODY_TXT)) {
                try (InputStreamReader reader = new InputStreamReader(input.openStream())) {
                    data.setBody(Joiner.on('\n').join(CharStreams.readLines(reader)));
                }
            } else if (input.getFilename().endsWith(HEADER_TXT)) {
                try (InputStreamReader reader = new InputStreamReader(input.openStream())) {
                    data.setHeading(Joiner.on('\n').join(CharStreams.readLines(reader)));
                }
            }
        }
        return data;
    }
}
