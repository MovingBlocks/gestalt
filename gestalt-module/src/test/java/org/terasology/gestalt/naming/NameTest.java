// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.gestalt.naming;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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
