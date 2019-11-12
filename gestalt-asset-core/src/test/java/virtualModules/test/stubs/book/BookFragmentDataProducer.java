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

package virtualModules.test.stubs.book;

import org.terasology.gestalt.assets.AbstractFragmentDataProducer;
import org.terasology.gestalt.assets.ResourceUrn;
import org.terasology.gestalt.assets.management.AssetManager;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import virtualModules.test.stubs.text.TextData;

/**
 * @author Immortius
 */
public class BookFragmentDataProducer extends AbstractFragmentDataProducer<TextData, Book, BookData> {

    public BookFragmentDataProducer(AssetManager assetManager) {
        super(assetManager, Book.class, true);
    }

    @Override
    protected Optional<TextData> getFragmentData(ResourceUrn urn, Book book) {
        try {
            int i = Integer.parseInt(urn.getFragmentName().toString());
            if (i >= 0 && i < book.getLines().size()) {
                return Optional.of(new TextData(book.getLines().get(i)));
            }
            return Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    @Override
    public Set<ResourceUrn> getAvailableAssetUrns() {
        return Collections.emptySet();
    }
}
