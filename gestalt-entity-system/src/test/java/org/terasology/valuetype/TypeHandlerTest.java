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

package org.terasology.valuetype;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

/**
 *
 */
public class TypeHandlerTest {

    @Test
    public void immutableCopy() {
        TypeHandler<String> handler = new TypeHandler<>(String.class, ImmutableCopy.create());
        String value = "dsflksdjlkf";
        assertSame(value, handler.copy(value));
    }

    @Test
    public void trueCopy() {
        TypeHandler<AtomicInteger> handler = new TypeHandler<>(AtomicInteger.class, x -> new AtomicInteger(x.get()));
        AtomicInteger value = new AtomicInteger(23);
        AtomicInteger copy = handler.copy(value);
        assertNotSame(value, copy);
        assertEquals(value.get(), copy.get());
    }
}
