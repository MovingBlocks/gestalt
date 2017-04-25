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
public class SimpleHandlerTest {

    private final Object input;
    private final Object output;
    private final Type type;
    private final ProtoTypeHandler handler;
    private final ProtoPersistence context = ProtoPersistence.create();


    @Parameterized.Parameters(name = "{0} - {2}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                // String
                {null, null, String.class, new StringHandler()},
                {"Test", "Test", String.class, new StringHandler()},
                // Double
                {null, 0.0, Double.class, new DoubleHandler()},
                {0.0, 0.0, Double.class, new DoubleHandler()},
                {1.32, 1.32, Double.class, new DoubleHandler()},
                // Float
                {null, 0f, Float.class, new FloatHandler()},
                {0f, 0f, Float.class, new FloatHandler()},
                {1.2f, 1.2f, Float.class, new FloatHandler()},
                // Integer
                {null, 0, Integer.class, new IntegerHandler()},
                {0, 0, Integer.class, new IntegerHandler()},
                {12, 12, Integer.class, new IntegerHandler()},
                // Long
                {null, 0L, Long.class, new LongHandler()},
                {0L, 0L, Long.class, new LongHandler()},
                {12L, 12L, Long.class, new LongHandler()},
                // Boolean
                {null, false, Boolean.class, new BooleanHandler()},
                {true, true, Boolean.class, new BooleanHandler()},
                {false, false, Boolean.class, new BooleanHandler()},
                // Byte
                {null, (byte) 0, Byte.class, new ByteHandler()},
                {(byte) 0, (byte) 0, Byte.class, new ByteHandler()},
                {(byte) 12, (byte) 12, Byte.class, new ByteHandler()},
                // Character
                {null, '\0', Character.class, new CharHandler()},
                {'a', 'a', Character.class, new CharHandler()},

        });
    }

    public SimpleHandlerTest(Object input, Object output, Type type, ProtoTypeHandler handler) {
        this.input = input;
        this.output = output;
        this.handler = handler;
        this.type = type;
    }

    @Test
    public void test() {
        assertEquals(output, handler.deserialize(handler.serialize(input, type, context).build(), type, context));
    }


}
