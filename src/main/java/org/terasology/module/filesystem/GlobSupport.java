/*
 * Copyright 2014 MovingBlocks
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

package org.terasology.module.filesystem;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * Provides support for glob expressions when working with ModuleFileSystems.
 * @author Immortius
 */
public final class GlobSupport {

    private GlobSupport() {
    }

    /**
     * Converts a glob expression into the equivalent regular expression for use with module file systems.
     * @param glob A glob expression compatible with ModuleFileSystem.
     * @return The equivalent regular expression to glob.
     */
    public static String globToRegex(String glob) {
        StringBuilder regex = new StringBuilder();
        int index = 0;
        while (index < glob.length()) {
            char c = glob.charAt(index++);
            switch (c) {
                case '*':
                    if (index < glob.length() && glob.charAt(index) == '*') {
                        regex.append(".*");
                        index++;
                    } else {
                        regex.append("[^/]*");
                    }
                    break;
                case '?':
                    regex.append("[^/]");
                    break;
                case '\\':
                    if (index < glob.length()) {
                        switch (glob.charAt(index)) {
                            case '[':
                            case '{':
                            case '*':
                            case '?':
                            case '\\':
                                regex.append("\\");
                                regex.append(glob.charAt(index++));
                                break;
                            default:
                                regex.append(glob.charAt(index++));
                        }
                    } else {
                        throw new IllegalArgumentException("Expected character to escape after '\\'");
                    }
                    break;
                case '{':
                    index = extractSubpatterns(index, glob, regex);
                    break;
                case '[':
                    index = startExtractRange(index, glob, regex);
                    break;
                case '.':
                case '^':
                case '$':
                case '+':
                case '(':
                case '|':
                    regex.append("\\");
                    regex.append(c);
                    break;
                default:
                    regex.append(c);
                    break;
            }
        }
        return regex.toString();
    }

    private static int startExtractRange(int startingIndex, String glob, StringBuilder regex) {
        char c = glob.charAt(startingIndex);
        switch (c) {
            case ']':
                throw new IllegalArgumentException("Empty ranges not supported");
            case '!':
                regex.append("[[^/]&&[^");
                return extractRange(startingIndex + 1, glob, regex);
            default:
                regex.append("[[^/]&&[");
                return extractRange(startingIndex, glob, regex);
        }
    }

    private static int extractRange(int startingIndex, String glob, StringBuilder regex) {
        int index = startingIndex;
        while (index < glob.length()) {
            char c = glob.charAt(index++);
            switch (c) {
                case '\\':
                case '[':
                    regex.append('\\');
                    regex.append(c);
                    break;
                case ']':
                    regex.append("]]");
                    return index;
                default:
                    regex.append(c);
            }
        }
        throw new IllegalArgumentException("Incomplete range group, expected ']'");
    }

    private static int extractSubpatterns(int startingIndex, String glob, StringBuilder regex) {
        List<String> subexpressions = Lists.newArrayList();
        StringBuilder subpattern = new StringBuilder();
        int index = startingIndex;
        while (index < glob.length()) {
            char c = glob.charAt(index++);
            switch (c) {
                case '\\':
                    if (index < glob.length()) {
                        subpattern.append('\\');
                        subpattern.append(glob.charAt(index++));
                    } else {
                        throw new IllegalArgumentException("Expected character to escape after '\\'");
                    }
                    break;
                case '{':
                    throw new IllegalArgumentException("Nested subpattern groups are not allowed");
                case ',':
                    if (subpattern.length() > 0) {
                        subexpressions.add(globToRegex(subpattern.toString()));
                        subpattern.setLength(0);
                    }
                    break;
                case '}':
                    if (subpattern.length() > 0) {
                        subexpressions.add(globToRegex(subpattern.toString()));
                    }
                    if (!subexpressions.isEmpty()) {
                        if (subexpressions.size() == 1) {
                            regex.append(subexpressions.get(0));
                        } else {
                            regex.append('(');
                            regex.append(Joiner.on("|").join(subexpressions));
                            regex.append(')');
                        }
                    }
                    return index;
                default:
                    subpattern.append(c);
            }
        }
        throw new IllegalArgumentException("Incomplete subpattern group, expected '}'");
    }
}
