// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.gestalt.naming;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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
