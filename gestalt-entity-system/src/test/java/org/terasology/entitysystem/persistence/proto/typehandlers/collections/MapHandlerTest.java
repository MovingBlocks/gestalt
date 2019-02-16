/*
 * Copyright 2019 MovingBlocks
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

package org.terasology.entitysystem.persistence.proto.typehandlers.collections;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;

import org.junit.Test;
import org.terasology.entitysystem.persistence.proto.ProtoPersistence;
import org.terasology.entitysystem.persistence.proto.typehandlers.StringHandler;
import org.terasology.entitysystem.persistence.proto.typehandlers.primitives.IntegerHandler;

import java.lang.reflect.Type;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class MapHandlerTest {

    private static Type STRING_INTEGER_MAP = new TypeToken<Map<String, Integer>>() {
    }.getType();

    private static Map<String, Integer> TEST = ImmutableMap.of("Test", 1, "Green", 2);

    private MapHandler handler = new MapHandler();
    private ProtoPersistence context = ProtoPersistence.create();

    public MapHandlerTest() {
        context.addTypeHandler(handler, Map.class);
        context.addTypeHandler(new StringHandler(), String.class);
        context.addTypeHandler(new IntegerHandler(), Integer.class);
    }

    @Test
    public void emptyMap() {
        Map deserialize = handler.deserialize(handler.serialize(Maps.newHashMap(), STRING_INTEGER_MAP, context).build(), STRING_INTEGER_MAP, context);
        assertTrue(deserialize.isEmpty());
    }

    @Test
    public void testMap() {
        Map deserialize = handler.deserialize(handler.serialize(TEST, STRING_INTEGER_MAP, context).build(), STRING_INTEGER_MAP, context);
        assertEquals(TEST, deserialize);
    }
}
