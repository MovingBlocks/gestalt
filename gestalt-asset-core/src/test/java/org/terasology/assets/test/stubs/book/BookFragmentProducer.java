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

import com.google.common.base.Optional;
import org.terasology.assets.AbstractFragmentProducer;
import org.terasology.assets.AssetManager;
import org.terasology.assets.test.stubs.text.TextData;
import org.terasology.naming.ResourceUrn;

import java.util.Collections;
import java.util.Set;

/**
 * @author Immortius
 */
public class BookFragmentProducer extends AbstractFragmentProducer<TextData, Book, BookData> {

    public BookFragmentProducer(AssetManager assetManager) {
        super(assetManager, Book.class);
    }

    @Override
    protected Optional<TextData> getFragmentData(ResourceUrn urn, Book book) {
        try {
            int i = Integer.parseInt(urn.getFragmentName().toString());
            if (i >= 0 && i < book.getLines().size()) {
                return Optional.of(new TextData(book.getLines().get(i)));
            }
            return Optional.absent();
        } catch (NumberFormatException e) {
            return Optional.absent();
        }
    }

    @Override
    public Set<ResourceUrn> getAvailableAssetUrns() {
        return Collections.emptySet();
    }
}
