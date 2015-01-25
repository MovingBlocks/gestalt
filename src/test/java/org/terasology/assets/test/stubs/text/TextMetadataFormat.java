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

package org.terasology.assets.test.stubs.text;

import com.google.common.base.Joiner;
import com.google.common.io.CharStreams;
import org.terasology.assets.AssetInput;
import org.terasology.assets.module.AbstractAssetAlterationFormat;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

/**
 * @author Immortius
 */
public class TextMetadataFormat extends AbstractAssetAlterationFormat<TextData> {

    public TextMetadataFormat() {
        super("info");
    }

    @Override
    public void apply(List<AssetInput> inputs, TextData assetData) throws IOException {
        if (!inputs.isEmpty()) {
            try (InputStreamReader reader = new InputStreamReader(inputs.get(0).openStream())) {
                String metadata = Joiner.on("/n").join(CharStreams.readLines(reader));
                assetData.setMetadata(metadata);
            }
        }
    }
}
