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

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.assets.Asset;
import org.terasology.assets.AssetType;
import org.terasology.assets.ResourceUrn;

import java.util.Optional;

/**
 * @author Immortius
 */
public class Book extends Asset<BookData> {

    private ImmutableList<String> lines = ImmutableList.of();

    public Book(ResourceUrn urn, BookData data, AssetType<?, BookData> type) {
        super(urn, type, new DisposalAction(urn));
        reload(data);
    }

    @Override
    protected Optional<? extends Asset<BookData>> doCreateCopy(ResourceUrn instanceUrn, AssetType<?, BookData> parentAssetType) {
        return Optional.of(new Book(instanceUrn, new BookData(lines), parentAssetType));
    }

    @Override
    protected void doReload(BookData data) {
        lines = ImmutableList.copyOf(data.getLines());
    }

    public ImmutableList<String> getLines() {
        return lines;
    }

    private static class DisposalAction implements Runnable {

        private static final Logger logger = LoggerFactory.getLogger(DisposalAction.class);
        private ResourceUrn urn;

        public DisposalAction(ResourceUrn urn) {
            this.urn = urn;
        }

        @Override
        public void run() {
            logger.info("Disposed: {}", urn);
        }
    }
}
