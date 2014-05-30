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

package org.terasology.module;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.terasology.i18n.I18nMap;
import org.terasology.i18n.gson.I18nMapTypeAdapter;
import org.terasology.naming.Name;
import org.terasology.naming.Version;
import org.terasology.naming.gson.NameTypeAdapter;
import org.terasology.naming.gson.VersionTypeAdapter;

import java.io.Reader;

/**
 * Reads ModuleMetadata from a json format.
 * An example of this format is:
 * <pre>
 *     {
 *          "id": "Core",
 *          "version": "0.1.0",
 *          "displayName": {
 *              "en": "Short Name"
 *          },
 *          "description": {
 *              "en": "A longer description of the module"
 *          },
 *          "serverSideOnly": false,
 *          "dependencies": [
 *              {
 *                  "id": "baseModule",
 *                  "minVersion": "1.0.0",
 *                  "maxVersion": "2.0.0"
 *              }
 *          ]
 *     }
 * </pre>
 *
 * @author Immortius
 */
public class ModuleMetadataReader {

    private final Gson gson;
    private final Class<? extends ModuleMetadata> metadataClass;

    public ModuleMetadataReader() {
        this(ModuleMetadata.class);
    }

    public ModuleMetadataReader(Class<? extends ModuleMetadata> metadataClass) {
        this.metadataClass = metadataClass;
        this.gson = new GsonBuilder()
                .registerTypeAdapter(Version.class, new VersionTypeAdapter())
                .registerTypeAdapter(Name.class, new NameTypeAdapter())
                .registerTypeAdapter(I18nMap.class, new I18nMapTypeAdapter())
                .create();
    }

    /**
     * @param reader A reader providing the json metadata
     * @return The ModuleMetadata represented by the JSON
     * @throws com.google.gson.JsonIOException     if there was a problem reading from the Reader
     * @throws com.google.gson.JsonSyntaxException if json is not valid
     */
    public ModuleMetadata read(Reader reader) {
        return gson.fromJson(reader, metadataClass);
    }
}
