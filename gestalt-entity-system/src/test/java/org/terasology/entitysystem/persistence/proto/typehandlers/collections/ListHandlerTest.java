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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;

import org.junit.Test;
import org.terasology.entitysystem.persistence.proto.ProtoPersistence;
import org.terasology.entitysystem.persistence.proto.typehandlers.StringHandler;
import org.terasology.entitysystem.persistence.protodata.ProtoDatastore;

import java.lang.reflect.Type;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 *
 */
public class ListHandlerTest {

    private static Type STRING_LIST = new TypeToken<List<String>>() {
    }.getType();

    private static List<String> TEST = ImmutableList.of("Value1", "Value2", "Value3");

    private ListHandler handler = new ListHandler();
    private ProtoPersistence context = ProtoPersistence.create();

    public ListHandlerTest() {
        context.addTypeHandler(handler, List.class);
        context.addTypeHandler(new StringHandler(), String.class);
    }

    @Test
    public void serializeEmptyList() {
        ProtoDatastore.Value.Builder data = handler.serialize(Lists.newArrayList(), STRING_LIST, context);
        assertNotNull(data);
        assertEquals(0, data.getValueCount());
    }

    @Test
    public void deserializeEmptyList() {
        ProtoDatastore.Value data = ProtoDatastore.Value.newBuilder().build();
        assertEquals(Lists.newArrayList(), handler.deserialize(data, STRING_LIST, context));
    }

    @Test
    public void serializeList() {
        ProtoDatastore.Value.Builder data = handler.serialize(TEST, STRING_LIST, context);
        assertNotNull(data);
        assertEquals(TEST.size(), data.getStringCount());
        for (int i = 0; i < TEST.size(); i++) {
            assertEquals(TEST.get(i), data.getString(i));
        }
    }

    @Test
    public void deserializeList() {
        ProtoDatastore.Value.Builder dataBuilder = ProtoDatastore.Value.newBuilder();
        for (String item : TEST) {
            dataBuilder.addString(item);
        }
        assertEquals(TEST, handler.deserialize(dataBuilder.build(), STRING_LIST, context));
    }

    @Test(expected = IllegalArgumentException.class)
    public void serializeListWithoutParameterizedType() {
        handler.serialize(TEST, List.class, context);
    }

    @Test(expected = IllegalArgumentException.class)
    public void deserializeListWithoutParameterizedType() {
        ProtoDatastore.Value.Builder dataBuilder = ProtoDatastore.Value.newBuilder();
        for (String item : TEST) {
            dataBuilder.addValue(ProtoDatastore.Value.newBuilder().addString(item));
        }
        handler.deserialize(dataBuilder.build(), List.class, context);
    }


}
