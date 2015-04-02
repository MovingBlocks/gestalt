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

package org.terasology.naming;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * @author Immortius
 */
public class NameTest {

    @Test
    public void trivialNameEquality() {
        assertEquals(new Name("Hello"), new Name("Hello"));
    }

    @Test
    public void differentCaseNameEquality() {
        assertEquals(new Name("hello"), new Name("HELLO"));
    }

    @Test
    public void maintainOriginalCase() {
        assertEquals("HeLLO", new Name("HeLLO").toString());
    }

    @Test
    public void differentNamesNotEqual() {
        assertNotEquals(new Name("hello"), new Name("Goodbye"));
    }
}
