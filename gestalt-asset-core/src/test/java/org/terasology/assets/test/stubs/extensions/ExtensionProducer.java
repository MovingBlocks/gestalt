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

package org.terasology.assets.test.stubs.extensions;

import com.google.common.base.Optional;
import org.terasology.assets.AssetManager;
import org.terasology.assets.AssetProducer;
import org.terasology.assets.module.annotations.RegisterAssetProducer;
import org.terasology.assets.test.stubs.text.TextData;
import org.terasology.naming.Name;
import org.terasology.naming.ResourceUrn;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

/**
 * @author Immortius
 */
@RegisterAssetProducer
public class ExtensionProducer implements AssetProducer<TextData> {

    public ExtensionProducer(AssetManager assetManager) {
    }

    @Override
    public Set<ResourceUrn> getAvailableAssetUrns() {
        return Collections.emptySet();
    }

    @Override
    public Set<ResourceUrn> resolve(String urn, Name moduleContext) {
        return Collections.emptySet();
    }

    @Override
    public ResourceUrn redirect(ResourceUrn urn) {
        return urn;
    }

    @Override
    public Optional<TextData> getAssetData(ResourceUrn urn) throws IOException {
        return Optional.absent();
    }

    @Override
    public void close() {
    }
}
