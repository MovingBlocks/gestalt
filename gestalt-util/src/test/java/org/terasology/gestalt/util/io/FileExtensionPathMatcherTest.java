// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

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
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * @author Immortius
 */
public class FileExtensionPathMatcherTest {
    public static Stream<Arguments> data() {
        return Stream.of(
                arguments(Arrays.asList("txt"), Paths.get("shroud.dat"), false),
                arguments(Arrays.asList("txt"), Paths.get("shroud.txt"), true),
                arguments(Arrays.asList("txt", "rbl"), Paths.get("shroud.txt"), true),
                arguments(Arrays.asList("txt", "rbl"), Paths.get("shroud.rbl"), true),
                arguments(Arrays.asList("txt", "rbl"), Paths.get("shroud.mrr"), false)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    public void test(List<String> extensions, Path testPath, boolean shouldMatch) {
        assertEquals(shouldMatch, new FileExtensionPathMatcher(extensions).matches(testPath));
    }
}
