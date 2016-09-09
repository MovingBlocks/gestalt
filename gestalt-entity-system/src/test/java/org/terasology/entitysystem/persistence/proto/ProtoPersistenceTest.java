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

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import org.junit.Test;
import org.terasology.entitysystem.persistence.proto.exception.PersistenceException;
import org.terasology.entitysystem.persistence.proto.typehandlers.StringHandler;
import org.terasology.entitysystem.persistence.proto.typehandlers.collections.ListHandler;
import org.terasology.entitysystem.persistence.protodata.ProtoDatastore;

import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class ProtoPersistenceTest {

    private static Type STRING_LIST = new TypeToken<List<String>>() {
    }.getType();
    private static Type STRING_SUBTYPE_LIST = new TypeToken<List<? extends String>>() {
    }.getType();
    private static Type STRING_ARRAYLIST = new TypeToken<ArrayList<String>>() {
    }.getType();

    private static final String TEST_STRING = "Test";
    private static final List<String> TEST_LIST = Lists.newArrayList("One", "Two");

    private ProtoPersistence protoPersistence;

    public ProtoPersistenceTest() {
        protoPersistence = new ProtoPersistence();
    }

    @Test(expected = PersistenceException.class)
    public void failsToSerializeUnknownType() throws MalformedURLException {
        protoPersistence.serialize(new URL("http://example.com"), URL.class);
    }

    @Test
    public void serializeWithSimpleTypeHandler() {
        protoPersistence.addTypeHandler(new StringHandler(), String.class);
        String result = protoPersistence.deserialize(protoPersistence.serialize(TEST_STRING, String.class).build(), String.class);
        assertEquals(TEST_STRING, result);
    }

    @Test
    public void serializeWithClassTypeHandler() {
        protoPersistence.addTypeHandler(new StringHandler(), String.class);
        protoPersistence.addTypeHandler(new ListHandler(), List.class);
        Object result = protoPersistence.deserialize(protoPersistence.serialize(TEST_LIST, STRING_LIST).build(), STRING_LIST);
        assertEquals(TEST_LIST, result);
    }

    @Test
    public void serializeWithParameterizedTypeHandler() {
        protoPersistence.addTypeHandler(new StringHandler(), String.class);
        protoPersistence.addTypeHandler(new ListHandler(), STRING_LIST);
        Object result = protoPersistence.deserialize(protoPersistence.serialize(TEST_LIST, STRING_LIST).build(), STRING_LIST);
        assertEquals(TEST_LIST, result);
    }

    @Test
    public void serializeWithWildcardType() {
        protoPersistence.addTypeHandler(new StringHandler(), String.class);
        protoPersistence.addTypeHandler(new ListHandler(), List.class);
        Object result = protoPersistence.deserialize(protoPersistence.serialize(TEST_LIST, STRING_SUBTYPE_LIST).build(), STRING_SUBTYPE_LIST);
        assertEquals(TEST_LIST, result);
    }

    @Test
    public void serializeWithTypeFactory() {
        protoPersistence.addTypeHandler(new StringHandler(), String.class);
        protoPersistence.addTypeHandlerFactory(new StubTypeHandlerFactory());
        Object result = protoPersistence.deserialize(protoPersistence.serialize(TEST_LIST, STRING_ARRAYLIST).build(), STRING_ARRAYLIST);
        assertEquals(TEST_LIST, result);
    }

    @Test
    public void getIdFromLookupTable() {
        int id = protoPersistence.getIdFor("table", "one");
        assertEquals(id, protoPersistence.getIdFor("table", "one"));
    }

    @Test
    public void getValueFromLookupTable() {
        int id = protoPersistence.getIdFor("table", "one");
        assertEquals("one", protoPersistence.getValueFor("table", id));
    }

    private static final class StubTypeHandlerFactory implements ProtoTypeHandlerFactory {

        private ListHandler innerHandler = new ListHandler();

        @Override
        public <T> ProtoTypeHandler<T> createTypeHandler(TypeToken<T> type) {
            if (type.getRawType() == ArrayList.class) {
                return new ProtoTypeHandler<T>() {
                    @Override
                    public ProtoDatastore.Value.Builder serialize(T instance, Type type, ProtoContext context) {
                        return innerHandler.serialize((List) instance, type, context);
                    }

                    @Override
                    public T deserialize(ProtoDatastore.Value value, Type type, ProtoContext context) {
                        return (T) innerHandler.deserialize(value, type, context);
                    }
                };
            } else {
                return null;
            }
        }
    }
}
