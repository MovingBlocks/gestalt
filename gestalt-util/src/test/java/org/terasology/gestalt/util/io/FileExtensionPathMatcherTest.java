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

package org.terasology.gestalt.util.io;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Immortius
 */
public class FileExtensionPathMatcherTest {

    static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(Arrays.asList("txt"), Paths.get("shroud.dat"), false),
                Arguments.of(Arrays.asList("txt"), Paths.get("shroud.txt"), true),
                Arguments.of(Arrays.asList("txt", "rbl"), Paths.get("shroud.txt"), true),
                Arguments.of(Arrays.asList("txt", "rbl"), Paths.get("shroud.rbl"), true),
                Arguments.of(Arrays.asList("txt", "rbl"), Paths.get("shroud.mrr"), false)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    public void test(List<String> extensions, Path testPath, boolean shouldMatch) {
        FileExtensionPathMatcher matcher = new FileExtensionPathMatcher(extensions);
        assertEquals(shouldMatch, matcher.matches(testPath));
    }
}
