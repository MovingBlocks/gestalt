package org.terasology.gestalt.graphics;

/**
 * Interface to a read-only view of a Color.
 */
public interface Colorc {
    /**
     * The internal representation of the color
     * @return hex representation
     */
    int rgba();

    /**
     * the internal representation of the color with alpha channel set to 0xFF
     * @return hex representation
     */
    int rgb();

    /**
     * @return The red component, between 0 and 255
     */
    int r();

    /**
     * @return The green component, between 0 and 255
     */
    int g();

    /**
     * @return The blue component, between 0 and 255
     */
    int b();

    /**
     * @return The alpha component, between 0 and 255
     */
    int a();

    /**
     * @return The red channel, between 0.0f and 1.0f
     */
    float rf();

    /**
     * @return The green channel, between 0.0f and 1.0f
     */
    float gf();

    /**
     * @return The blue channel, between 0.0f and 1.0f
     */
    float bf();

    /**
     * @return The alpha channel, between 0.0f and 1.0f
     */
    float af();

    /**
     * the hex representation of color as a String
     * @return the hex representation
     */
    String toHex();
}
