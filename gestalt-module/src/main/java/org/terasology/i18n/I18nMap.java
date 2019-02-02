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

package org.terasology.i18n;

import com.google.common.collect.ImmutableMap;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javax.annotation.concurrent.Immutable;

/**
 * I18nMap is a map of Strings by Locale, to support lookup fields that can have different values for different languages (internationalized fields).
 * <p>
 * This is not intended to be used as a replacement of Java's existing internationalisation support for strings, but instead used for
 * internationalised strings read from external sources.
 * </p>
 * <p>
 * If a String is not available for a particular locale, then it will fallback though the following steps to find one:
 * </p>
 * <ol>
 * <li>Drop Locale variant</li>
 * <li>Drop Locale country</li>
 * <li>Use system default display Locale</li>
 * <li>Use English Locale</li>
 * <li>Use any available Locale</li>
 * </ol>
 *
 * @author Immortius
 */
@Immutable
public class I18nMap implements Iterable<Map.Entry<Locale, String>> {
    private final Map<Locale, String> values;

    /**
     * Constructor when a mapping of Locale to String is available.
     *
     * @param values A map of locale-string values.
     */
    public I18nMap(Map<Locale, String> values) {
        this.values = ImmutableMap.copyOf(values);
    }

    /**
     * Constructor when only a single String is available - this will be registered against the default Locale.
     *
     * @param value The sole value of this map.
     */
    public I18nMap(String value) {
        values = ImmutableMap.of(Locale.getDefault(Locale.Category.DISPLAY), value);
    }

    /**
     * @return The most appropriate string value to use based on the system default Locale
     */
    public String value() {
        String result = values.get(Locale.getDefault(Locale.Category.DISPLAY));
        if (result == null) {
            result = values.get(Locale.ENGLISH);
        }
        if (result == null && !values.isEmpty()) {
            result = values.values().iterator().next();
        }
        if (result == null) {
            result = "";
        }
        return result;
    }

    /**
     * @param locale The locale to get the string value for
     * @return The most appropriate string value for the given locale.
     */
    public String valueFor(Locale locale) {
        String result = values.get(locale);
        if (result == null && !locale.getVariant().isEmpty()) {
            Locale fallbackLocale = new Locale(locale.getLanguage(), locale.getCountry());
            result = values.get(fallbackLocale);
        }
        if (result == null && !locale.getCountry().isEmpty()) {
            Locale fallbackLocale = new Locale(locale.getLanguage());
            result = values.get(fallbackLocale);
        }
        if (result == null) {
            result = value();
        }
        return result;
    }

    /**
     * @return The most appropriate string value based on the system default locale.
     */
    @Override
    public String toString() {
        return value();
    }

    @Override
    public Iterator<Map.Entry<Locale, String>> iterator() {
        return values.entrySet().iterator();
    }

    @Override
    public int hashCode() {
        return Objects.hash(values);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof I18nMap) {
            I18nMap other = (I18nMap) obj;

            return Objects.equals(values, other.values);
        }

        return false;
    }
}
