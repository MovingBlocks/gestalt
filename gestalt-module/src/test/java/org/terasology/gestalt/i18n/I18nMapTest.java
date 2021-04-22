// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.gestalt.i18n;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Immortius
 */
public class I18nMapTest {
    private static final Locale AUSTRALIAN_STRINE_LOCALE = new Locale("en", "au", "strine");
    private static final Locale AUSTRALIAN_LOCALE = new Locale("en", "au");
    private static final Locale ENGLISH_LOCALE = Locale.ENGLISH;

    private static final Locale DEFAULT_LOCALE = Locale.GERMAN;

    private static Locale originalDefault;

    @BeforeAll
    public static void before() {
        originalDefault = Locale.getDefault(Locale.Category.DISPLAY);
        Locale.setDefault(Locale.Category.DISPLAY, DEFAULT_LOCALE);
    }

    @AfterAll
    public static void after() {
        Locale.setDefault(Locale.Category.DISPLAY, originalDefault);
    }

    @Test
    public void simpleI18nMap() {
        I18nMap value = new I18nMap("Hello");
        assertEquals("Hello", value.toString());
    }

    @Test
    public void exactLocaleMatch() {
        I18nMap value = new I18nMap(ImmutableMap.<Locale, String>builder()
                .put(AUSTRALIAN_STRINE_LOCALE, "g'day")
                .put(AUSTRALIAN_LOCALE, "hello")
                .put(ENGLISH_LOCALE, "greetings")
                .put(DEFAULT_LOCALE, "guten morgen")
                .put(Locale.ITALIAN, "ciao").build());
        assertEquals("g'day", value.valueFor(AUSTRALIAN_STRINE_LOCALE));
        assertEquals("hello", value.valueFor(AUSTRALIAN_LOCALE));
        assertEquals("greetings", value.valueFor(ENGLISH_LOCALE));
    }

    @Test
    public void fallbackToCountry() {
        I18nMap value = new I18nMap(ImmutableMap.<Locale, String>builder()
                .put(AUSTRALIAN_LOCALE, "hello")
                .put(ENGLISH_LOCALE, "greetings")
                .put(DEFAULT_LOCALE, "guten morgen")
                .put(Locale.ITALIAN, "ciao").build());
        assertEquals("hello", value.valueFor(AUSTRALIAN_STRINE_LOCALE));
    }

    @Test
    public void fallbackToLanguage() {
        I18nMap value = new I18nMap(ImmutableMap.<Locale, String>builder()
                .put(ENGLISH_LOCALE, "greetings")
                .put(DEFAULT_LOCALE, "guten morgen")
                .put(Locale.ITALIAN, "ciao").build());
        assertEquals("greetings", value.valueFor(AUSTRALIAN_STRINE_LOCALE));
        assertEquals("greetings", value.valueFor(AUSTRALIAN_LOCALE));
    }

    @Test
    public void fallbackEnglish() {
        I18nMap value = new I18nMap(ImmutableMap.<Locale, String>builder()
                .put(ENGLISH_LOCALE, "greetings")
                .put(Locale.ITALIAN, "ciao").build());
        assertEquals("greetings", value.valueFor(Locale.CHINA));
    }

    @Test
    public void fallbackDefault() {
        I18nMap value = new I18nMap(ImmutableMap.<Locale, String>builder()
                .put(ENGLISH_LOCALE, "greetings")
                .put(DEFAULT_LOCALE, "guten morgen")
                .put(Locale.ITALIAN, "ciao").build());
        assertEquals("guten morgen", value.valueFor(Locale.CHINA));
    }

    @Test
    public void fallbackToAvailable() {
        I18nMap value = new I18nMap(ImmutableMap.<Locale, String>builder()
                .put(Locale.ITALIAN, "ciao").build());
        assertEquals("ciao", value.valueFor(Locale.CHINA));
    }

    @Test
    public void emptyStringIfUnresolvable() {
        I18nMap value = new I18nMap(Collections.<Locale, String>emptyMap());
        assertEquals("", value.valueFor(Locale.CHINA));
    }
}
