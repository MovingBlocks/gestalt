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

package org.terasology.assets;

import com.google.common.base.Optional;
import org.terasology.naming.Name;

import java.io.Closeable;
import java.io.IOException;
import java.util.Set;

/**
 * AssetDataProducers are the sources of asset data used to produce assets.  They also allow are used to resolve partial urns and redirect urns to other assets.
 * @author Immortius
 */
public interface AssetDataProducer<T extends AssetData> extends Closeable {

    /**
     * Optionally can provide a set of ResourceUrns this AssetDataProducer can provide data for.  It is not required by the asset system, and is intended only for
     * displays of available assets. If it is infeasible to provide such a list (such as when the AssetDataProducer procedurally generates assets based on part of the
     * urn) an empty set can be returned instead.
     * @return A set that may contain the urns of assets this producer can provide data for.
     */
    Set<ResourceUrn> getAvailableAssetUrns();

    /**
     * The names of modules for which this producer can produce asset data with the given resource name for.
     * @param resourceName The name of a resource
     * @return A set of modules containing the resource.
     */
    Set<Name> getModulesProviding(Name resourceName);

    ResourceUrn redirect(ResourceUrn urn);

    Optional<T> getAssetData(ResourceUrn urn) throws IOException;

    @Override
    void close();


}
