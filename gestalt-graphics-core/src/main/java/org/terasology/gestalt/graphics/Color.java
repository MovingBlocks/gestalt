// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.gestalt.graphics;

import org.joml.Math;
import org.joml.Vector3fc;
import org.joml.Vector3ic;
import org.joml.Vector4fc;
import org.joml.Vector4ic;

import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.Objects;

/**
 * Color is a representation of a RGBA color. Color components can be set and accessed via floats ranging from 0-1, or ints ranging from 0-255.
 * Color is immutable and thread safe.
 * <br><br>
 * There are a plethora of Color classes, but none that are quite suitable IMO:
 * <ul>
 * <li>vecmaths - doesn't access with r/g/b/a, separation by representation is awkward, feature bland.</li>
 * <li>Slick2D - ideally will lose dependency on slick utils. Also ties to lwjgl</li>
 * <li>Lwjgl - don't want to be graphics implementation dependant</li>
 * <li>javafx - ew</li>
 * <li>com.sun.prism - double ew. Shouldn't use com.sun classes at all</li>
 * <li>awt - tempting, certainly feature-rich. Has some strange awt-specific functionality though (createContext) and native links</li>
 * </ul>
 *
 */
public class Color implements Colorc {
    public static final Colorc black = new Color(0x000000FF);
    public static final Colorc white = new Color(0xFFFFFFFF);
    public static final Colorc blue = new Color(0x0000FFFF);
    public static final Colorc green = new Color(0x00FF00FF);
    public static final Colorc red = new Color(0xFF0000FF);
    public static final Colorc grey = new Color(0x888888FF);
    public static final Colorc transparent = new Color(0x00000000);
    public static final Colorc yellow = new Color(0xFFFF00FF);
    public static final Colorc cyan = new Color(0x00FFFFFF);
    public static final Colorc magenta = new Color(0xFF00FFFF);


    private static final int MAX = 255;
    private static final int RED_OFFSET = 24;
    private static final int GREEN_OFFSET = 16;
    private static final int BLUE_OFFSET = 8;
    private static final int RED_FILTER = 0x00FFFFFF;
    private static final int GREEN_FILTER = 0xFF00FFFF;
    private static final int BLUE_FILTER = 0xFFFF00FF;
    private static final int ALPHA_FILTER = 0xFFFFFF00;

    private int representation;

    /**
     * Creates a color that is black with full alpha.
     */
    public Color() {
        representation = 0x000000FF;
    }

    /**
     * range between 0x00000000 to 0xFFFFFFFF
     *
     * @param representation color in hex format
     */
    public Color(int representation) {
        this.representation = representation;
    }

    /**
     * set the color source
     *
     * @param src color source
     */
    public Color(Colorc src) {
        this.set(src.rgba());
    }

    /**
     * Create a color with the given red/green/blue values. Alpha is initialised as max.
     *
     * @param r red in the range of 0.0f to 1.0f
     * @param g green in the range of 0.0f to 1.0f
     * @param b blue in the range of 0.0f to 1.0f
     */
    public Color(float r, float g, float b) {
        this((int) (r * MAX), (int) (g * MAX), (int) (b * MAX));
    }

    /**
     * Creates a color with the given red/green/blue/alpha values.
     *
     * @param r red in the range of 0.0f to 1.0f
     * @param g green in the range of 0.0f to 1.0f
     * @param b blue in the range of 0.0f to 1.0f
     * @param a alpha in the range of 0.0f to 1.0f
     */
    public Color(float r, float g, float b, float a) {
        this((int) (r * MAX), (int) (g * MAX), (int) (b * MAX), (int) (a * MAX));
    }

    /**
     * Creates a color with the given red/green/blue values. Alpha is initialised as max.
     *
     * @param r red in the range of 0.0f to 1.0f
     * @param g green in the range of 0.0f to 1.0f
     * @param b blue in the range of 0.0f to 1.0f
     */
    public Color(int r, int g, int b) {
        this.set(r, g, b);
    }

    /**
     * Creates a color with the given red/green/blue/alpha values.
     *
     * @param r red in the range of 0 to 255
     * @param g green in the range of 0 to 255
     * @param b blue in the range of 0 to 255
     * @param a alpha in the range of 0 to 255
     */
    public Color(int r, int g, int b, int a) {
        this.set(r, g, b, a);
    }

    @Override
    public int r() {
        return (representation >> RED_OFFSET) & MAX;
    }

    @Override
    public int g() {
        return (representation >> GREEN_OFFSET) & MAX;
    }

    @Override
    public int b() {
        return (representation >> BLUE_OFFSET) & MAX;
    }

    @Override
    public int a() {
        return representation & MAX;
    }

    @Override
    public float rf() {
        return r() / 255.f;
    }

    @Override
    public float bf() {
        return b() / 255.f;
    }

    @Override
    public float gf() {
        return g() / 255.f;
    }

    @Override
    public float af() {
        return a() / 255.f;
    }


    public Color set(Vector3ic representation) {
        return this.set(representation.x(),
                representation.y(),
                representation.z());
    }

    public Color set(Vector3fc representation) {
        return this.set((int) (representation.x() * MAX),
                (int) (representation.y() * MAX),
                (int) (representation.z() * MAX));
    }


    public Color set(Vector4fc representation) {
        return this.set((int) (representation.x() * MAX),
                (int) (representation.y() * MAX),
                (int) (representation.z() * MAX),
                (int) (representation.w() * MAX));
    }

    public Color set(Vector4ic representation) {
        return this.set(representation.x(),
                representation.y(),
                representation.z(),
                representation.w());
    }

    public Color set(int representation) {
        this.representation = representation;
        return this;
    }

    public Color set(int r, int g, int b, int a) {
        return this.set(Math.clamp(0, 255, r) << RED_OFFSET |
                Math.clamp(0, 255, g) << GREEN_OFFSET |
                Math.clamp(0, 255, b) << BLUE_OFFSET |
                Math.clamp(0, 255, a));
    }


    public Color set(int r, int g, int b) {
        return this.set(r, g, b, 0xFF);
    }


    /**
     * set the value of the red channel
     *
     * @param value color range between 0-255
     * @return this
     */
    public Color setRed(int value) {
        return this.set(Math.clamp(0, 255, value) << RED_OFFSET | (representation & RED_FILTER));
    }

    /**
     * set the value of the red channel
     *
     * @param value color range between 0.0f to 1.0f
     * @return this
     */
    public Color setRed(float value) {
        return setRed((int) (value * MAX));
    }

    /**
     * set the value of the green channel
     *
     * @param value color range between 0-255
     * @return this
     */
    public Color setGreen(int value) {
        return this.set(Math.clamp(0, 255, value) << GREEN_OFFSET | (representation & GREEN_FILTER));
    }


    /**
     * set the value of the green channel
     *
     * @param value color range between 0.0f to 1.0f
     * @return this
     */
    public Color setGreen(float value) {
        return setGreen((int) (value * MAX));
    }


    /**
     * set the value of the blue channel
     *
     * @param value blue range between 0-255
     * @return this
     */
    public Color setBlue(int value) {
        return this.set(Math.clamp(0, 255, value) << BLUE_OFFSET | (representation & BLUE_FILTER));
    }

    /**
     * set the value of the blue channel
     *
     * @param value blue range between 0.0f to 1.0f
     * @return this
     */
    public Color setBlue(float value) {
        return setBlue((int) (value * MAX));
    }

    /**
     * set the value of the alpha channel
     *
     * @param value alpha range between 0-255
     * @return this
     */
    public Color setAlpha(int value) {
        return this.set(Math.clamp(0, 255, value) | (representation & ALPHA_FILTER));
    }

    /**
     * set the value of the alpha channel
     *
     * @param value alpha range between 0.0f to 1.0f
     * @return this
     */
    public Color setAlpha(float value) {
        return setAlpha((int) (value * MAX));
    }


    /**
     * 255 Subtract from all components except alpha;
     *
     * @return this
     */
    public Color invert() {
        return this.set((~representation & ALPHA_FILTER) | a());
    }

    @Override
    public int rgba() {
        return representation;
    }

    @Override
    public int rgb() {
        return (representation & ALPHA_FILTER) | 0xFF;
    }

    /**
     * write color to ByteBuffer as int.
     *
     * @param buffer The ByteBuffer
     */
    public void addToBuffer(ByteBuffer buffer) {
        buffer.putInt(representation);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Color) {
            Color other = (Color) obj;
            return representation == other.representation;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(representation);
    }

    @Override
    public String toHex() {
        StringBuilder builder = new StringBuilder();
        String hexString = Integer.toHexString(representation);
        for (int i = 0; i < 8 - hexString.length(); ++i) {
            builder.append('0');
        }
        builder.append(hexString.toUpperCase(Locale.ENGLISH));
        return builder.toString();
    }

    @Override
    public String toString() {
        return toHex();
    }
}
