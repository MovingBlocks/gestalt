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

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import org.terasology.naming.Name;
import org.terasology.naming.ResourceUrn;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

/**
 * @author Immortius
 */
public abstract class AbstractFragmentProducer<T extends AssetData, U extends Asset<V>, V extends AssetData> implements AssetProducer<T> {

    private final AssetManager assetManager;
    private final Class<U> rootAssetType;

    public AbstractFragmentProducer(AssetManager assetManager, Class<U> rootAssetType) {
        this.assetManager = assetManager;
        this.rootAssetType = rootAssetType;
    }

    @Override
    public Set<ResourceUrn> resolve(final String urn, Name moduleContext) {
        final int fragmentStart = urn.indexOf(ResourceUrn.FRAGMENT_SEPARATOR);
        if (fragmentStart >= 0) {
            String nonFragmentUrn = urn.substring(0, fragmentStart);
            return Sets.newLinkedHashSet(Collections2.transform(assetManager.resolve(nonFragmentUrn, rootAssetType, moduleContext), new Function<ResourceUrn, ResourceUrn>() {
                @Nullable
                @Override
                public ResourceUrn apply(ResourceUrn input) {
                    return new ResourceUrn(input.toString() + urn.substring(fragmentStart));
                }
            }));
        }
        return Collections.emptySet();
    }

    @Override
    public ResourceUrn redirect(ResourceUrn urn) {
        return urn;
    }

    @Override
    public T getAssetData(ResourceUrn urn) throws IOException {
        U rootAsset = assetManager.getAsset(urn.getRootUrn(), rootAssetType);
        if (rootAsset != null) {
            return getFragmentData(urn, rootAsset);
        }
        return null;
    }

    protected abstract T getFragmentData(ResourceUrn urn, U rootAsset);
}
