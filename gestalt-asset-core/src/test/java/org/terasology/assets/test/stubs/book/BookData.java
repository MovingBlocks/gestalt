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

package org.terasology.assets.test.stubs.book;

import com.google.common.collect.Lists;
import org.terasology.assets.AssetData;

import java.util.Arrays;
import java.util.List;

/**
 * @author Immortius
 */
public class BookData implements AssetData {
    private final List<String> lines = Lists.newArrayList();
    private String author;
    private String body;

    public BookData() {
    }

    public BookData(List<String> lines) {
        this.lines.clear();
        this.lines.addAll(lines);
    }

    public BookData(String... lines) {
        this.lines.addAll(Arrays.asList(lines));
    }

    public List<String> getLines() {
        return lines;
    }

    public void setLines(List<String> lines) {
        this.lines.clear();
        this.lines.addAll(lines);
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
