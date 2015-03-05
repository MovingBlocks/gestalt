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

package org.terasology.util.io;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Immortius
 */
@RunWith(Parameterized.class)
public class FileExtensionPathMatcherTest {
    private FileExtensionPathMatcher matcher;
    private Path testPath;
    private boolean shouldMatch;

    public FileExtensionPathMatcherTest(List<String> extensions, Path testPath, boolean shouldMatch) {
        this.matcher = new FileExtensionPathMatcher(extensions);
        this.testPath = testPath;
        this.shouldMatch = shouldMatch;
    }

    @Parameterized.Parameters
    public static Collection data() {
        return Arrays.asList(new Object[][]{
                {Arrays.asList("txt"), Paths.get("shroud.dat"), false},
                {Arrays.asList("txt"), Paths.get("shroud.txt"), true},
                {Arrays.asList("txt", "rbl"), Paths.get("shroud.txt"), true},
                {Arrays.asList("txt", "rbl"), Paths.get("shroud.rbl"), true},
                {Arrays.asList("txt", "rbl"), Paths.get("shroud.mrr"), false}
        });
    }

    @Test
    public void test() {
        assertEquals(shouldMatch, matcher.matches(testPath));
    }
}
