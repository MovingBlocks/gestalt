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

package org.terasology.entitysystem.core;

import com.google.common.base.Preconditions;
import org.terasology.util.collection.TypeKeyedMap;

import java.util.Optional;
import java.util.Set;

/**
 * A proxy entity ref wraps another entity ref - which can be changed. This allows for a new entity ref to be returned, and then later switched to a real entity ref after
 * the current transaction is committed (or switched to a {@link NullEntityRef} if the transaction is rolled back.
 *
 * Note that a proxy entity ref is not 'equals' to the entity it is proxy for - as the entity ref it is a proxy for can change. If the ProxyEntityRef took identity from
 * what it proxied this would cause issues using them within Sets or as keys for Maps.
 */
public class ProxyEntityRef implements EntityRef {

    private EntityRef ref;

    /**
     * @param ref The entity ref to proxy.
     */
    public ProxyEntityRef(EntityRef ref) {
        Preconditions.checkNotNull(ref);
        this.ref = ref;
    }

    /**
     * @return The {@link EntityRef} that this reference proxies.
     */
    public EntityRef getActualRef() {
        return ref;
    }

    /**
     * Changes the reference to proxy.
     * @param ref The new reference to proxy
     */
    public void setActualRef(EntityRef ref) {
        Preconditions.checkNotNull(ref);
        this.ref = ref;
    }

    @Override
    public long getId() {
        return ref.getId();
    }

    @Override
    public long getRevision() {
        return ref.getRevision();
    }

    @Override
    public boolean isPresent() {
        return ref.isPresent();
    }

    @Override
    public <T extends Component> Optional<T> getComponent(Class<T> componentType) {
        return ref.getComponent(componentType);
    }

    @Override
    public Set<Class<? extends Component>> getComponentTypes() {
        return ref.getComponentTypes();
    }

    @Override
    public TypeKeyedMap<Component> getComponents() {
        return ref.getComponents();
    }

    @Override
    public <T extends Component> T addComponent(Class<T> componentType) {
        return ref.addComponent(componentType);
    }

    @Override
    public <T extends Component> void removeComponent(Class<T> componentType) {
        ref.removeComponent(componentType);
    }

    @Override
    public void delete() {
        ref.delete();
    }
}
