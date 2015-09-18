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

import org.terasology.assets.Asset;
import org.terasology.assets.AssetType;
import org.terasology.assets.ResourceUrn;

import java.util.Optional;

/**
 * @author Immortius
 */
public class Text extends Asset<TextData> {

    private String value;

    public Text(ResourceUrn urn, TextData data, AssetType<?, TextData> type) {
        super(urn, type);
        reload(data);
    }

    @Override
    protected Optional<? extends Asset<TextData>> doCreateCopy(ResourceUrn copyUrn, AssetType<?, TextData> parentAssetType) {
        return Optional.of(new Text(copyUrn, new TextData(value), parentAssetType));
    }

    @Override
    protected void doReload(TextData data) {
        value = data.getValue();
    }

    public String getValue() {
        return value;
    }
}
