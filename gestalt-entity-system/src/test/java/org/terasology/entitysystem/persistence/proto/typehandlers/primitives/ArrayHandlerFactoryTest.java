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

package org.terasology.entitysystem.persistence.proto.typehandlers.primitives;

import org.junit.Test;
import org.terasology.entitysystem.persistence.proto.ProtoPersistence;
import org.terasology.entitysystem.persistence.proto.typehandlers.StringHandler;

import static org.junit.Assert.assertArrayEquals;

/**
 *
 */
public class ArrayHandlerFactoryTest {

    private ProtoPersistence context = new ProtoPersistence();

    public ArrayHandlerFactoryTest() {
        context.addTypeHandler(new StringHandler(), String.class);
        context.addTypeHandler(new IntegerHandler(), Integer.class);
        context.addTypeHandler(new IntegerHandler(), Integer.TYPE);
        context.addTypeHandlerFactory(new ArrayHandlerFactory());
    }

    @Test
    public void serializeEmptyArray() {
        assertArrayEquals(new String[0], context.deserialize(context.serialize(new String[]{}).build(), String[].class));
    }

    @Test
    public void serializeStringArray() {
        assertArrayEquals(new String[]{"Hello", "World"}, context.deserialize(context.serialize(new String[]{"Hello", "World"}).build(), String[].class));
    }

    @Test
    public void serializeIntegerArray() {
        assertArrayEquals(new Integer[]{1, 2}, context.deserialize(context.serialize(new Integer[]{1, 2}).build(), Integer[].class));
    }

    @Test
    public void serializeIntArray() {
        assertArrayEquals(new int[]{1, 2}, context.deserialize(context.serialize(new int[]{1, 2}).build(), int[].class));
    }

    @Test
    public void serializeFloatArray() {
        float[] value = context.deserialize(context.serialize(new float[]{1f, 2f}).build(), float[].class);
        assertArrayEquals(new float[]{1f, 2f}, value, 0.0001f);
    }

    @Test
    public void serializeDoubleArray() {
        double[] value = context.deserialize(context.serialize(new double[]{1.0, 2.0}).build(), double[].class);
        assertArrayEquals(new double[]{1.0, 2.0}, value, 0.0001);
    }

    @Test
    public void serializeBooleanArray() {
        boolean[] value = context.deserialize(context.serialize(new boolean[]{true, false}).build(), boolean[].class);
        assertArrayEquals(new boolean[]{true, false}, value);
    }

    @Test
    public void serializeByteArray() {
        byte[] value = context.deserialize(context.serialize(new byte[]{(byte) 1, (byte) 2, (byte) 3, (byte) 4}).build(), byte[].class);
        assertArrayEquals(new byte[]{(byte) 1, (byte) 2, (byte) 3, (byte) 4}, value);
    }

    @Test
    public void serializeCharArray() {
        char[] value = context.deserialize(context.serialize(new char[]{'a', 'b'}).build(), char[].class);
        assertArrayEquals(new char[]{'a', 'b'}, value);
    }

}
