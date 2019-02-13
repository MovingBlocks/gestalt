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

package org.terasology.entitysystem.persistence.proto.typehandlers.collections;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;

import org.junit.Test;
import org.terasology.entitysystem.persistence.proto.ProtoPersistence;
import org.terasology.entitysystem.persistence.proto.typehandlers.StringHandler;
import org.terasology.entitysystem.persistence.protodata.ProtoDatastore;

import java.lang.reflect.Type;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 *
 */
public class SetHandlerTest {

    private static Type STRING_SET = new TypeToken<Set<String>>() {
    }.getType();

    private static Set<String> TEST = ImmutableSet.of("Value1", "Value2", "Value3");

    private SetHandler handler = new SetHandler();
    private ProtoPersistence context = ProtoPersistence.create();

    public SetHandlerTest() {
        context.addTypeHandler(handler, Set.class);
        context.addTypeHandler(new StringHandler(), String.class);
    }

    @Test
    public void serializeEmptySet() {
        ProtoDatastore.Value.Builder data = handler.serialize(Sets.newLinkedHashSet(), STRING_SET, context);
        assertNotNull(data);
        assertEquals(0, data.getValueCount());
    }

    @Test
    public void deserializeEmptySet() {
        ProtoDatastore.Value data = ProtoDatastore.Value.newBuilder().build();
        assertEquals(Sets.newLinkedHashSet(), handler.deserialize(data, STRING_SET, context));
    }

    @Test
    public void serializeSet() {
        ProtoDatastore.Value.Builder data = handler.serialize(TEST, STRING_SET, context);
        assertNotNull(data);
        assertEquals(TEST.size(), data.getStringCount());
        Set<String> values = Sets.newLinkedHashSet();
        for (int i = 0; i < TEST.size(); i++) {
            values.add(data.getString(i));
        }
        assertEquals(TEST, values);
    }

    @Test
    public void deserializeSet() {
        ProtoDatastore.Value.Builder dataBuilder = ProtoDatastore.Value.newBuilder();
        for (String item : TEST) {
            dataBuilder.addString(item);
        }
        assertEquals(TEST, handler.deserialize(dataBuilder.build(), STRING_SET, context));
    }

    @Test(expected = IllegalArgumentException.class)
    public void serializeSetWithoutParameterizedType() {
        handler.serialize(TEST, Set.class, context);
    }

    @Test(expected = IllegalArgumentException.class)
    public void deserializeSetWithoutParameterizedType() {
        ProtoDatastore.Value.Builder dataBuilder = ProtoDatastore.Value.newBuilder();
        for (String item : TEST) {
            dataBuilder.addValue(ProtoDatastore.Value.newBuilder().addString(item));
        }
        handler.deserialize(dataBuilder.build(), Set.class, context);
    }


}
