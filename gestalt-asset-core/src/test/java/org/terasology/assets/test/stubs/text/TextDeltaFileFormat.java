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
import com.google.common.io.CharStreams;
import org.terasology.assets.format.AssetDataFile;
import org.terasology.assets.format.AbstractAssetAlterationFileFormat;

import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author Immortius
 */
public class TextDeltaFileFormat extends AbstractAssetAlterationFileFormat<TextData> {

    public TextDeltaFileFormat() {
        super("delta");
    }

    @Override
    public void apply(AssetDataFile input, TextData assetData) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(input.openStream(), Charsets.UTF_8)) {
            for (String line : CharStreams.readLines(reader)) {
                String[] parts = line.split("->", 2);
                if (parts.length == 2) {
                    assetData.setValue(assetData.getValue().replace(parts[0], parts[1]));
                }
            }
        }
    }
}
