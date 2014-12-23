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

import com.google.common.collect.Lists;
import org.terasology.naming.ResourceUrn;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * @author Immortius
 */
public class UnloadedAsset<T extends AssetData> {

    private final List<AssetInput> inputs = Lists.newArrayList();
    private final AssetFormat<T> format;
    private final ResourceUrn urn;

    public UnloadedAsset(ResourceUrn urn, AssetFormat<T> format) {
        this.urn = urn;
        this.format = format;
    }

    public ResourceUrn getUrn() {
        return urn;
    }

    public void addInput(Path path) {
        inputs.add(new AssetInput(path));
    }

    public T load() throws IOException {
        return format.load(urn, inputs);
    }
}
