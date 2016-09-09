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

package org.terasology.entitysystem.persistence.proto.typehandlers;

import org.junit.Test;
import org.terasology.entitysystem.component.CodeGenComponentManager;
import org.terasology.entitysystem.component.ComponentManager;
import org.terasology.entitysystem.entity.EntityManager;
import org.terasology.entitysystem.entity.EntityRef;
import org.terasology.entitysystem.entity.inmemory.InMemoryEntityManager;
import org.terasology.entitysystem.entity.references.NewEntityRef;
import org.terasology.entitysystem.entity.references.NullEntityRef;
import org.terasology.entitysystem.persistence.proto.ProtoPersistence;
import org.terasology.entitysystem.stubs.SampleComponent;
import org.terasology.valuetype.TypeLibrary;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class PrefabEntityRefHandlerTest {


    private EntityManager entityManager;
    private ComponentManager componentManager;
    private ProtoPersistence context = new ProtoPersistence();

    public PrefabEntityRefHandlerTest() {
        TypeLibrary typeLibrary = new TypeLibrary();
        componentManager = new CodeGenComponentManager(typeLibrary);
        entityManager = new InMemoryEntityManager(componentManager);
        context.addTypeHandler(new EntityRefHandler(entityManager), EntityRef.class);
    }

    @Test
    public void handleNullEntityRef() {
        EntityRef ref = NullEntityRef.get();
        assertEquals(ref, context.deserialize(context.serialize(ref, EntityRef.class).build(), EntityRef.class));
    }

    @Test
    public void handleCoreEntityRef() {
        entityManager.beginTransaction();
        EntityRef ref = entityManager.createEntity();
        ref.addComponent(SampleComponent.class);
        entityManager.commit();

        ref = ((NewEntityRef) ref).getInnerEntityRef().get();

        assertEquals(ref, context.deserialize(context.serialize(ref, EntityRef.class).build(), EntityRef.class));
    }

    @Test
    public void handleNewEntityRef() {
        entityManager.beginTransaction();
        EntityRef ref = entityManager.createEntity();
        ref.addComponent(SampleComponent.class);
        entityManager.commit();

        assertEquals(((NewEntityRef) ref).getInnerEntityRef().get(), context.deserialize(context.serialize(ref, EntityRef.class).build(), EntityRef.class));
    }




}
