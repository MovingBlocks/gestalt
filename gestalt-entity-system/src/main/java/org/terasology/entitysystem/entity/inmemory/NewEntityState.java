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

package org.terasology.entitysystem.entity.inmemory;

import org.terasology.entitysystem.component.ComponentManager;
import org.terasology.entitysystem.core.Component;
import org.terasology.entitysystem.core.EntityRef;
import org.terasology.entitysystem.core.ProxyEntityRef;
import org.terasology.util.collection.TypeKeyedMap;

/**
 *
 */
public class NewEntityState {
    private long id;
    private NewEntityRef entityRef;
    private ProxyEntityRef proxyEntityRef;
    private EntityRef actualEntity;
    private TypeKeyedMap<Component> components;

    public NewEntityState(ComponentManager componentManager) {
        components = new TypeKeyedMap<>();
        entityRef = new NewEntityRef(componentManager, this);
        proxyEntityRef = new ProxyEntityRef(entityRef);
    }

    public ProxyEntityRef getProxyEntityRef() {
        return proxyEntityRef;
    }

    public EntityRef getActualEntity() {
        return actualEntity;
    }

    public void setActualEntity(EntityRef ref) {
        actualEntity = ref;
        proxyEntityRef.setActualRef(ref);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public TypeKeyedMap<Component> getComponents() {
        return components;
    }
}
