/*
 * Copyright 2016 MovingBlocks
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

package org.terasology.entitysystem.entity.references;

import org.terasology.entitysystem.entity.Component;
import org.terasology.entitysystem.entity.EntityManager;
import org.terasology.entitysystem.entity.EntityRef;
import org.terasology.util.collection.TypeKeyedMap;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The primary entity ref, references an entity within an {@link EntityManager}.
 * <p>
 * Interactions with the referenced entity are done through the active transaction on this thread. If there is no active transaction then an {@link IllegalStateException}
 * will be thrown.
 */
public class CoreEntityRef implements EntityRef {

    private EntityManager entityManager;
    private long id;

    /**
     * Constructs an entity ref
     * @param entityManager The entityManager the referenced entity exists within
     * @param id The id of the entity
     */
    public CoreEntityRef(EntityManager entityManager, long id) {
        this.entityManager = entityManager;
        this.id = id;
    }

    /**
     * @return The id of the entity referenced by this EntityRef
     */
    @Override
    public long getId() {
        return id;
    }

    /**
     * @return The entity manager the entity referenced lives within
     */
    public EntityManager getEntityManager() {
        return entityManager;
    }

    @Override
    public boolean isPresent() {
        return entityManager.getRawTransaction().exists(id);
    }

    @Override
    public <T extends Component> Optional<T> getComponent(Class<T> componentType) {
        return entityManager.getRawTransaction().getComponent(id, componentType);
    }

    @Override
    public Set<Class<? extends Component>> getComponentTypes() {
        return entityManager.getRawTransaction().getEntityComposition(id);
    }

    @Override
    public TypeKeyedMap<Component> getComponents() {
        return entityManager.getRawTransaction().getEntityComponents(id);
    }

    @Override
    public <T extends Component> T addComponent(Class<T> componentType) {
        return entityManager.getRawTransaction().addComponent(id, componentType);
    }

    @Override
    public <T extends Component> void removeComponent(Class<T> componentType) {
        entityManager.getRawTransaction().removeComponent(id, componentType);
    }

    @Override
    public void delete() {
        getComponentTypes().forEach(this::removeComponent);
    }

    @Override
    public String toString() {
        return "EntityRef( " + id + " )";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof CoreEntityRef) {
            CoreEntityRef other = (CoreEntityRef) obj;
            return id == other.id && Objects.equals(entityManager, other.entityManager);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
}
