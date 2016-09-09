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

import com.google.common.collect.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.terasology.entitysystem.persistence.proto.ProtoPersistence;
import org.terasology.entitysystem.persistence.proto.ProtoTypeHandler;
import org.terasology.entitysystem.persistence.proto.typehandlers.primitives.BooleanHandler;
import org.terasology.entitysystem.persistence.proto.typehandlers.primitives.ByteHandler;
import org.terasology.entitysystem.persistence.proto.typehandlers.primitives.CharHandler;
import org.terasology.entitysystem.persistence.proto.typehandlers.primitives.DoubleHandler;
import org.terasology.entitysystem.persistence.proto.typehandlers.primitives.FloatHandler;
import org.terasology.entitysystem.persistence.proto.typehandlers.primitives.IntegerHandler;
import org.terasology.entitysystem.persistence.proto.typehandlers.primitives.LongHandler;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

/**
 *
 */
@RunWith(Parameterized.class)
public class SimpleCollectionHandlerTest {

    private final Collection<Object> input;
    private final Collection<Object> output;
    private final Type type;
    private final ProtoTypeHandler handler;
    private final ProtoPersistence context = new ProtoPersistence();


    @Parameterized.Parameters(name = "{0} - {2}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                // String
                {Lists.newArrayList(), Lists.newArrayList(), String.class, new StringHandler()},
                {Arrays.asList("Test"), Arrays.asList("Test"), String.class, new StringHandler()},
                {Lists.newArrayList("Test", null), Arrays.asList("Test", ""), String.class, new StringHandler()},
                // Double
                {Lists.newArrayList(), Lists.newArrayList(), Double.class, new DoubleHandler()},
                {Arrays.asList(1.32, 2.46), Arrays.asList(1.32, 2.46), Double.class, new DoubleHandler()},
                // Float
                {Lists.newArrayList(), Lists.newArrayList(), Float.class, new FloatHandler()},
                {Arrays.asList(1.32f, 2.46f), Arrays.asList(1.32f, 2.46f), Float.class, new FloatHandler()},
                // Integer
                {Lists.newArrayList(), Lists.newArrayList(), Integer.class, new IntegerHandler()},
                {Arrays.asList(3, 7), Arrays.asList(3, 7), Float.class, new IntegerHandler()},
                // Long
                {Lists.newArrayList(), Lists.newArrayList(), Long.class, new LongHandler()},
                {Arrays.asList(3L, 7L), Arrays.asList(3L, 7L), Long.class, new LongHandler()},
                // Boolean
                {Lists.newArrayList(), Lists.newArrayList(), Boolean.class, new BooleanHandler()},
                {Arrays.asList(true, false), Arrays.asList(true, false), Boolean.class, new BooleanHandler()},
                // Byte
                {Lists.newArrayList(), Lists.newArrayList(), Byte.class, new ByteHandler()},
                {Arrays.asList((byte) 12, (byte) 123), Arrays.asList((byte) 12, (byte) 123), Byte.class, new ByteHandler()},
                // Character
                {Lists.newArrayList(), Lists.newArrayList(), Character.class, new CharHandler()},
                {Arrays.asList('a', 'b', 'c'), Arrays.asList('a', 'b', 'c'), Character.class, new CharHandler()},

        });
    }

    public SimpleCollectionHandlerTest(Collection<Object> input, Collection<Object> output, Type type, ProtoTypeHandler handler) {
        this.input = input;
        this.output = output;
        this.handler = handler;
        this.type = type;
    }

    @Test
    public void test() {
        assertEquals(output, handler.deserializeCollection(handler.serializeCollection(input, type, context).build(), type, context));
    }


}
