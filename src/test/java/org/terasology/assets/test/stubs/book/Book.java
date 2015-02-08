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

package org.terasology.assets.test.stubs.book;

import com.google.common.collect.ImmutableList;
import org.terasology.assets.Asset;
import org.terasology.naming.ResourceUrn;

/**
 * @author Immortius
 */
public class Book extends Asset<BookData> {

    private ImmutableList<String> lines = ImmutableList.of();

    public Book(ResourceUrn urn, BookData data) {
        super(urn);
        reload(data);
    }

    @Override
    protected Asset<BookData> doCreateInstance(ResourceUrn instanceUrn) {
        return new Book(instanceUrn, new BookData(lines));
    }

    @Override
    protected void doReload(BookData data) {
        lines = ImmutableList.copyOf(data.getLines());
    }

    @Override
    protected void doDispose() {
        lines = ImmutableList.of();
    }

    public ImmutableList<String> getLines() {
        return lines;
    }
}
