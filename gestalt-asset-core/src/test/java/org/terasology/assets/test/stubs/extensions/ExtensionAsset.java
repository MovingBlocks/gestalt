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

package org.terasology.assets.test.stubs.extensions;

import org.terasology.assets.Asset;
import org.terasology.naming.ResourceUrn;

/**
 * @author Immortius
 */
public class ExtensionAsset extends Asset<ExtensionData> {
    private String value;

    public ExtensionAsset(ResourceUrn urn, ExtensionData data) {
        super(urn);
        doReload(data);
    }

    @Override
    protected Asset<ExtensionData> doCreateInstance(ResourceUrn urn) {
        return new ExtensionAsset(urn, new ExtensionData(value));
    }

    @Override
    protected void doReload(ExtensionData data) {
        this.value = data.getValue();
    }

    @Override
    protected void doDispose() {

    }

    public String getValue() {
        return value;
    }
}
