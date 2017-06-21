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

package org.terasology.assets;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.terasology.assets.management.AssetManager;
import org.terasology.assets.management.AssetTypeManager;
import org.terasology.assets.test.VirtualModuleEnvironmentFactory;
import org.terasology.assets.test.stubs.book.Book;
import org.terasology.assets.test.stubs.book.BookData;
import org.terasology.assets.test.stubs.book.BookFactory;
import org.terasology.assets.test.stubs.book.BookFragmentDataProducer;
import org.terasology.assets.test.stubs.text.Text;
import org.terasology.assets.test.stubs.text.TextData;
import org.terasology.assets.test.stubs.text.TextFactory;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Immortius
 */
public class AbstractFragmentProducerTest {

    private static final ResourceUrn FRAGMENT_URN = new ResourceUrn("engine", "test", "0");
    private static final ResourceUrn FRAGMENT_URN_2 = new ResourceUrn("engine", "test", "1");
    private static final String LINE_0 = "hello";
    private static final String LINE_1 = "world";

    private AssetTypeManager assetTypeManager = mock(AssetTypeManager.class);
    private AssetManager assetManager = new AssetManager(assetTypeManager);
    private BookFragmentDataProducer bookFragmentProducer = new BookFragmentDataProducer(assetManager);
    private AssetType<Book, BookData> bookType = new AssetType<>(Book.class, new BookFactory());

    public AbstractFragmentProducerTest() throws Exception {
        when(assetTypeManager.getAssetType(Book.class)).thenReturn(Optional.of(bookType));
        when(assetTypeManager.getAssetTypes(Book.class)).thenReturn(ImmutableList.<AssetType<? extends Book, ?>>of(bookType));
    }

    @Test
    public void getAssetData() throws Exception {
        bookType.loadAsset(FRAGMENT_URN.getRootUrn(), new BookData(LINE_0, LINE_1));

        Optional<TextData> result = bookFragmentProducer.getAssetData(FRAGMENT_URN);
        assertTrue(result.isPresent());
        assertEquals(LINE_0, result.get().getValue());

        Optional<TextData> result2 = bookFragmentProducer.getAssetData(FRAGMENT_URN_2);
        assertTrue(result2.isPresent());
        assertEquals(LINE_1, result2.get().getValue());

    }

    @Test
    public void resolvePartialFragmentUrn() throws Exception {
        bookType.loadAsset(FRAGMENT_URN.getRootUrn(), new BookData(LINE_0, LINE_1));
        AssetDataProducer<BookData> bookProducer = mock(AssetDataProducer.class);
        when(bookProducer.getModulesProviding(FRAGMENT_URN.getResourceName())).thenReturn(ImmutableSet.of(FRAGMENT_URN.getModuleName()));
        bookType.addProducer(bookProducer);

        AssetType<Text, TextData> textType = new AssetType<>(Text.class, new TextFactory());
        textType.addProducer(bookFragmentProducer);
        when(assetTypeManager.getAssetType(Text.class)).thenReturn(Optional.of(textType));
        when(assetTypeManager.getAssetTypes(Text.class)).thenReturn(ImmutableList.<AssetType<? extends Text, ?>>of(textType));

        assertEquals(ImmutableSet.of(FRAGMENT_URN), textType.resolve(FRAGMENT_URN.getResourceName() + "#" + FRAGMENT_URN.getFragmentName()));
    }


}
