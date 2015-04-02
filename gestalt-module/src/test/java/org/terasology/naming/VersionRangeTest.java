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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Immortius
 */
public class VersionRangeTest {

    private VersionRange range = new VersionRange(new Version("1.2.3"), new Version("2.3.4"));

    @Test
    public void doesNotContainLowerVersion() {
        assertFalse(range.contains(new Version("1.0.0")));
        assertFalse(range.contains(new Version("1.2.2")));
        assertFalse(range.contains(new Version("1.1.5")));
    }

    @Test
    public void containsLowerBound() {
        assertTrue(range.contains(new Version("1.2.3")));
    }

    @Test
    public void containsVersions() {
        assertTrue(range.contains(new Version("1.6.0")));
        assertTrue(range.contains(new Version("2.0.0")));
        assertTrue(range.contains(new Version("2.3.3")));
    }

    @Test
    public void doesNotContainUpperBound() {
        assertFalse(range.contains(new Version("2.3.4")));
    }

    @Test
    public void doesNotContainHigherVersions() {
        assertFalse(range.contains(new Version("2.3.10")));
        assertFalse(range.contains(new Version("2.4.0")));
        assertFalse(range.contains(new Version("3.0.0")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void errorIfUpperBoundLowerThanLowerBound() {
        new VersionRange(new Version("2.0.0"), new Version("1.0.0"));
    }
}
