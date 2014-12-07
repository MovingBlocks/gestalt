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

package org.terasology.assets;

import org.junit.Test;
import org.terasology.assets.stubs.books.Book;
import org.terasology.assets.stubs.books.BookData;
import org.terasology.assets.stubs.books.BookFactory;
import org.terasology.naming.Name;
import org.terasology.naming.ResourceUrn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Immortius
 */
public class AssetTypeTest {

    public static final String ASSET_TYPE_ID = "book";
    public static final String FOLDER_NAME = "books";

    public static final ResourceUrn URN = new ResourceUrn("engine", "testAssetPleaseIgnore");


    private AssetType<Book, BookData> assetType = new AssetType<>(ASSET_TYPE_ID, FOLDER_NAME, Book.class);

    @Test
    public void construction() {

        assertEquals(new Name(ASSET_TYPE_ID), assetType.getId());
        assertEquals(FOLDER_NAME, assetType.getFolderName());
        assertEquals(Book.class,  assetType.getAssetClass());
    }

    @Test
    public void loadData() {
        AssetFactory<Book, BookData> factory = mock(AssetFactory.class);
        BookData data = new BookData("Title", "Body");
        assetType.setFactory(factory);
        Book book = new Book(URN, data);
        when(factory.build(URN, data)).thenReturn(book);

        Book createdBook = assetType.loadAsset(URN, data);
        assertEquals(book, createdBook);
        verify(factory).build(URN, data);
    }

    @Test
    public void retrieveLoadedDataByUrn() {
        assetType.setFactory(new BookFactory());
        BookData data = new BookData("Title", "Body");

        Book loadedBook = assetType.loadAsset(URN, data);
        Book retrievedBook = assetType.getAsset(URN);
        assertEquals(loadedBook, retrievedBook);
    }

    @Test
    public void loadingAssetWithSameUrnReloadsExistingAsset() {
        assetType.setFactory(new BookFactory());
        BookData initialData = new BookData("Title", "Body");
        Book initialBook = assetType.loadAsset(URN, initialData);
        BookData newData = new BookData("Title2", "Body2");

        Book newBook = assetType.loadAsset(URN, newData);
        assertSame(initialBook, newBook);
        assertEquals(newData.getHeading(), initialBook.getTitle());
        assertEquals(newData.getBody(), initialBook.getBody());
    }

    @Test
    public void changingFactoryDisposesAllAssets() {
        assetType.setFactory(new BookFactory());
        BookData data = new BookData("Title", "Body");
        Book asset = assetType.loadAsset(URN, data);

        assetType.setFactory(mock(AssetFactory.class));
        assertTrue(asset.isDisposed());
        assertNull(assetType.getAsset(URN));
    }

    @Test
    public void disposingAsset() {
        assetType.setFactory(new BookFactory());
        BookData data = new BookData("Title", "Body");
        Book asset = assetType.loadAsset(URN, data);

        assetType.dispose(URN);
        assertTrue(asset.isDisposed());
        assertNull(assetType.getAsset(URN));
    }

}
