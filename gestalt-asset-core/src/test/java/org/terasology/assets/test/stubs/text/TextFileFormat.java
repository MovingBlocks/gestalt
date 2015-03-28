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

package org.terasology.assets.test.stubs.text;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.CharStreams;
import org.terasology.assets.format.AbstractAssetFileFormat;
import org.terasology.assets.format.AssetDataFile;
import org.terasology.assets.ResourceUrn;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

/**
 * @author Immortius
 */
public class TextFileFormat extends AbstractAssetFileFormat<TextData> {
    private static final String TXT_EXTENSION = "txt";

    public TextFileFormat() {
        super(TXT_EXTENSION);
    }

    @Override
    public TextData load(ResourceUrn urn, List<AssetDataFile> inputs) throws IOException {
        TextData data = new TextData();
        if (!inputs.isEmpty()) {
            try (InputStreamReader reader = new InputStreamReader(inputs.get(0).openStream(), Charsets.UTF_8)) {
                data.setValue(Joiner.on('\n').join(CharStreams.readLines(reader)));
            }
        }
        return data;
    }
}
