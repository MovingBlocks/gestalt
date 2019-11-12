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

package org.terasology.gestalt.naming;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * @author Immortius
 */
public class NameVersionTest {

    private static final Name NAME_1 = new Name("NameOne");
    private static final Version V1 = new Version(1, 0, 0);
    private static final Name NAME_2 = new Name("NameTwo");
    private static final Version V2 = new Version(2, 0, 0);

    @Test
    public void retainsName() {
        assertEquals(NAME_1, new NameVersion(NAME_1, V1).getName());
    }

    @Test
    public void retainVersion() {
        assertEquals(V1, new NameVersion(NAME_1, V1).getVersion());
    }

    @Test
    public void equality() {
        assertEquals(new NameVersion(NAME_1, V1), new NameVersion(NAME_1, V1));
    }

    @Test
    public void equalityFailsIfNameDiffers() {
        assertNotEquals(new NameVersion(NAME_1, V1), new NameVersion(NAME_2, V1));
    }

    @Test
    public void equalityFailsIfVersionDiffers() {
        assertNotEquals(new NameVersion(NAME_1, V1), new NameVersion(NAME_1, V2));
    }

}
