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
import org.terasology.module.sandbox.API;
import org.terasology.naming.Name;

import java.io.Closeable;
import java.io.IOException;
import java.util.Set;

/**
 * AssetDataProducers provide asset data used to produce assets.
 * <p>
 * As the source of asset data, they also play a role in resolving partial urns and redirecting urns to other assets.
 * </p>
 * <p>
 * AssetDataProducer is closable, and should be closed when no longer in use, so that any file system handles (or similar) can be closed.
 * </p>
 *
 * @author Immortius
 */
@API
public interface AssetDataProducer<T extends AssetData> extends Closeable {

    /**
     * Optionally can provide a set of ResourceUrns this AssetDataProducer can provide data for.  It is not required by the asset system, and is intended only for
     * displays of available assets. If it is infeasible to provide such a list (such as when the AssetDataProducer procedurally generates assets based on part of the
     * urn) an empty set can be returned instead.
     *
     * @return A set that may contain the urns of assets this producer can provide data for.
     */
    Set<ResourceUrn> getAvailableAssetUrns();

    /**
     * The names of modules for which this producer can produce asset data with the given resource name for.
     *
     * @param resourceName The name of a resource
     * @return A set of modules containing the resource.
     */
    Set<Name> getModulesProviding(Name resourceName);

    /**
     * Gives the AssetDataProducer the opportunity to "redirect" the urn to another urn. If the asset data producer does not wish to do so it should return the original
     * urn.
     * <p>
     * A redirected urn indicates a different asset should be loaded to that requested. This can be used to leave breadcrumbs as assets are renamed so that
     * dependant modules can still discover the asset with the old name.
     * </p>
     *
     * @param urn The urn to redirect
     * @return Either the original urn, or a urn to redirect to.
     */
    ResourceUrn redirect(ResourceUrn urn);

    /**
     * @param urn The urn to get AssetData for
     * @return An optional with the AssetData, if available
     * @throws IOException If there is an error producing the AssetData.
     */
    Optional<T> getAssetData(ResourceUrn urn) throws IOException;

    @Override
    void close();


}
