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

package org.terasology.entitysystem.persistence.proto;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.TIntObjectMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitysystem.component.ComponentManager;
import org.terasology.entitysystem.component.ComponentType;
import org.terasology.entitysystem.component.PropertyAccessor;
import org.terasology.entitysystem.component.module.ComponentTypeIndex;
import org.terasology.entitysystem.entity.Component;
import org.terasology.entitysystem.persistence.protodata.ProtoDatastore;

import java.util.Optional;

/**
 *
 */
public class ComponentPersistor {

    private static final Logger logger = LoggerFactory.getLogger(ComponentPersistor.class);

//    message Component {
//        optional int32 type_index = 1; // Index of the type, if contained in a world with component_classes table
//        repeated int32 field_id = 2; // The field ids for this component
//        repeated Value field_values = 3; // The field values for this component
//
//        extensions 5000 to max;
//    }

    private final ComponentTypeIndex componentTypeIndex;
    private final ComponentManager componentManager;
    private final BiMap<Integer, ComponentType<?>> componentIdMap;


    public ComponentPersistor(ComponentTypeIndex componentTypeIndex, ComponentManager componentManager, TIntObjectMap<String> componentIdMap) {
        this.componentTypeIndex = componentTypeIndex;
        this.componentManager = componentManager;
        this.componentIdMap = HashBiMap.create();
        TIntObjectIterator<String> entryIterator = componentIdMap.iterator();
        while (entryIterator.hasNext()) {
            entryIterator.advance();
            Optional<Class<? extends Component>> componentType = componentTypeIndex.find(entryIterator.value());
            if (componentType.isPresent()) {
                this.componentIdMap.put(entryIterator.key(), componentManager.getType(componentType.get()));
            } else {
                logger.error("Failed to resolve component '{}', instances will not be deserialized", entryIterator.value());
            }
        }
    }


    public ProtoDatastore.Component serialize(Component component) {
        ComponentType<? extends Component> componentType = componentManager.getType(component.getClass());
        ProtoDatastore.Component.Builder builder = ProtoDatastore.Component.newBuilder();
        builder.setTypeIndex(componentIdMap.inverse().get(componentType));
        for (PropertyAccessor<? extends Component, ?> property : componentType.getPropertyInfo().getProperties().values()) {

        }


    }


    public Optional<Component> deserialize(ProtoDatastore.Component componentData) {
        return null;
    }
}
