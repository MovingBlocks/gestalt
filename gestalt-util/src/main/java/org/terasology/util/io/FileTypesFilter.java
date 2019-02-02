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

package org.terasology.util.io;

import android.support.annotation.RequiresApi;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A filter on one or more file types, as determined by the file's extension.
 *
 * @author Immortius
 */
@RequiresApi(26)
public class FileTypesFilter implements DirectoryStream.Filter<Path> {

    private final ImmutableList<String> fileTypes;

    /**
     * @param fileType  The first file type
     * @param fileTypes Zero or more additional file types
     */
    public FileTypesFilter(String fileType, String... fileTypes) {
        Preconditions.checkNotNull(fileType);
        Preconditions.checkNotNull(fileTypes);
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        if (!fileType.startsWith(".")) {
            builder.add("." + fileType);
        } else {
            builder.add(fileType);
        }
        for (String type : fileTypes) {
            if (!type.startsWith(".")) {
                builder.add("." + type);
            } else {
                builder.add(type);
            }
        }
        this.fileTypes = builder.build();

    }

    @Override
    public boolean accept(Path entry) throws IOException {
        if (Files.isRegularFile(entry)) {
            String entryAsString = entry.toString();
            for (String type : fileTypes) {
                if (entryAsString.endsWith(type)) {
                    return true;
                }
            }
        }
        return false;
    }
}
