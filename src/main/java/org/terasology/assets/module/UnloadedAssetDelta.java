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

package org.terasology.assets.module;

import com.google.common.collect.Lists;
import org.terasology.assets.AssetData;
import org.terasology.assets.AssetInput;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * @author Immortius
 */
class UnloadedAssetDelta<T extends AssetData> {
    private final List<AssetInput> inputs = Lists.newArrayList();
    private final AssetDeltaFormat<T> format;

    public UnloadedAssetDelta(AssetDeltaFormat<T> format) {
        this.format = format;
    }

    public void addInput(Path path) {
        inputs.add(new AssetInput(path));
    }

    public void applyTo(T data) throws IOException {
        format.applyDelta(inputs, data);
    }
}
