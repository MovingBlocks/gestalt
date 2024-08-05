package org.terasology.gestalt.graphics;

import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ColorTest {

    static Stream<Arguments> singleColorChangeIntArgs() {
        return Stream.of(
                Arguments.of(255, 255),
                Arguments.of(175, 175),
                Arguments.of(125, 125),
                Arguments.of(600, 255),
                Arguments.of(0, 0)
        );
    };

    static Stream<Arguments> setColorVector4fArgs() {
        return Stream.of(
                Arguments.of(new Vector4f(1.0f,0,0,0), new Color(255, 0,0,0)),
                Arguments.of(new Vector4f(0,1.0f,0,0), new Color(0, 255,0,0)),
                Arguments.of(new Vector4f(0,0,1.0f,0.0f), new Color(0, 0,255,0)),
                Arguments.of(new Vector4f(0,0,0.0f,1.0f), new Color(0, 0,0,255))
        );
    }

    static Stream<Arguments> setColorVector3fArgs() {
        return Stream.of(
                Arguments.of(new Vector3f(1.0f,0,0), new Color(255, 0,0,255)),
                Arguments.of(new Vector3f(0,1.0f,0), new Color(0, 255,0,255)),
                Arguments.of(new Vector3f(0,0,1.0f), new Color(0, 0,255,255))
        );
    }

    static Stream<Arguments> SingleColorChangeFloatArgs() {
        return Stream.of(
                Arguments.of(1.0f, 255),
                Arguments.of(0.68f, 173),
                Arguments.of(0.49f, 124),
                Arguments.of(6.0f, 255),
                Arguments.of(-1.0f, 0),
                Arguments.of(0, 0)
        );
    };

    static Stream<Arguments> inverseColorArgs() {
        return Stream.of(
                Arguments.of(new Color(255, 0, 0, 255), new Color(0, 255, 255, 255)),
                Arguments.of(new Color(0, 125, 0, 255), new Color(255, 130, 255, 255)),
                Arguments.of(new Color(125, 125, 0, 255), new Color(130, 130, 255, 255)),
                Arguments.of(new Color(0, 0, 90, 255), new Color(255, 255, 165, 255))
        );
    };

    static Stream<Arguments> hexColorArgs() {
        return Stream.of(
                Arguments.of(new Color(255, 0, 0, 255),"FF0000FF"),
                Arguments.of(new Color(0, 125, 0, 255), "007D00FF"),
                Arguments.of(new Color(125, 125, 0, 255), "7D7D00FF"),
                Arguments.of(new Color(0, 0, 90, 255), "00005AFF")
        );
    };


    @ParameterizedTest
    @MethodSource("hexColorArgs")
    public void testHexColor(Color c, String expected) {
        Color c1 = new Color(c);
        assertEquals(c1.toHex(), expected);
    }


    @ParameterizedTest
    @MethodSource("singleColorChangeIntArgs")
    public void testSetColorR(int test, int expected) {
        Color c1 = new Color();
        c1.setRed(test);

        assertEquals(expected, c1.r());
        assertEquals(0, c1.g());
        assertEquals(0, c1.b());
        assertEquals(255, c1.a());
    }


    @ParameterizedTest
    @MethodSource("singleColorChangeIntArgs")
    public void testSetColorG(int value, int expected) {
        Color c1 = new Color();
        c1.setGreen(value);

        assertEquals(0, c1.r());
        assertEquals(expected, c1.g());
        assertEquals(0, c1.b());
        assertEquals(255, c1.a());
    }

    @ParameterizedTest
    @MethodSource("singleColorChangeIntArgs")
    public void testSetColorB(int value, int expected) {
        Color c1 = new Color();
        c1.setBlue(value);

        assertEquals(0, c1.r());
        assertEquals(0, c1.g());
        assertEquals(expected, c1.b());
        assertEquals(255, c1.a());

    }

    @ParameterizedTest
    @MethodSource("singleColorChangeIntArgs")
    public void testSetColorA(int value, int expected) {
        Color c1 = new Color();
        c1.setAlpha(value);

        assertEquals(0, c1.r());
        assertEquals(0, c1.g());
        assertEquals(0, c1.b());
        assertEquals(expected, c1.a());
    }

    @ParameterizedTest
    @MethodSource("singleColorChangeIntArgs")
    public void testIntColorR(int value, int expected) {
        Color c1 = new Color(value, 0, 0);

        assertEquals(expected, c1.r());
        assertEquals(0, c1.g());
        assertEquals(0, c1.b());
        assertEquals(255, c1.a());
    }

    @ParameterizedTest
    @MethodSource("singleColorChangeIntArgs")
    public void testIntColorG(int value, int expected) {
        Color c2 = new Color(0, value, 0);

        assertEquals(c2.r(), 0);
        assertEquals(c2.g(), expected);
        assertEquals(c2.b(), 0);
        assertEquals(c2.a(), 255);
    }

    @ParameterizedTest
    @MethodSource("singleColorChangeIntArgs")
    public void testIntColorB(int value, int expected) {
        Color c3 = new Color(0, 0, value);

        assertEquals(c3.r(), 0);
        assertEquals(c3.g(), 0);
        assertEquals(c3.b(), expected);
        assertEquals(c3.a(), 255);
    }

    @ParameterizedTest
    @MethodSource("singleColorChangeIntArgs")
    public void testIntColorA(int value, int expected) {
        Color c3 = new Color(0, 0, 0,value);

        assertEquals(c3.r(), 0);
        assertEquals(c3.g(), 0);
        assertEquals(c3.b(), 0);
        assertEquals(c3.a(), expected);
    }

    @ParameterizedTest
    @MethodSource("SingleColorChangeFloatArgs")
    public void testFloatColorR(float value, int expected) {
        Color c2 = new Color(value, 0, 0);

        assertEquals(c2.r(), expected);
        assertEquals(c2.g(), 0);
        assertEquals(c2.b(), 0);
        assertEquals(c2.a(), 255);
    }

    @ParameterizedTest
    @MethodSource("SingleColorChangeFloatArgs")
    public void testFloatColorG(float value, int expected) {
        Color c2 = new Color(0, value, 0);

        assertEquals(c2.r(), 0);
        assertEquals(c2.g(), expected);
        assertEquals(c2.b(), 0);
        assertEquals(c2.a(), 255);
    }

    @ParameterizedTest
    @MethodSource("SingleColorChangeFloatArgs")
    public void testFloatColorB(float value, int expected) {
        Color c2 = new Color(0, 0.0f, value);

        assertEquals(c2.r(), 0);
        assertEquals(c2.g(), 0);
        assertEquals(c2.b(), expected);
        assertEquals(c2.a(), 255);
    }


    @ParameterizedTest
    @MethodSource("SingleColorChangeFloatArgs")
    public void testSetFloatColorA(float value, int expected) {
        Color test = new Color();
        test.setAlpha(value);

        assertEquals(test.r(), 0);
        assertEquals(test.g(), 0);
        assertEquals(test.b(), 0);
        assertEquals(test.a(), expected);
    }

    @ParameterizedTest
    @MethodSource("SingleColorChangeFloatArgs")
    public void testSetFloatColorR(float value, int expected) {
        Color test = new Color();
        test.setRed(value);

        assertEquals(test.r(), expected);
        assertEquals(test.g(), 0);
        assertEquals(test.b(), 0);
        assertEquals(test.a(), 255);
    }

    @ParameterizedTest
    @MethodSource("SingleColorChangeFloatArgs")
    public void testSetFloatColorG(float value, int expected) {
        Color c2 = new Color();
        c2.setGreen(value);

        assertEquals(c2.r(), 0);
        assertEquals(c2.g(), expected);
        assertEquals(c2.b(), 0);
        assertEquals(c2.a(), 255);
    }

    @ParameterizedTest
    @MethodSource("SingleColorChangeFloatArgs")
    public void testSetFloatColorB(float value, int expected) {
        Color c2 = new Color();
        c2.setBlue(value);

        assertEquals(c2.r(), 0);
        assertEquals(c2.g(), 0);
        assertEquals(c2.b(), expected);
        assertEquals(c2.a(), 255);
    }


    @ParameterizedTest
    @MethodSource("SingleColorChangeFloatArgs")
    public void testFloatColorA(float value, int expected) {
        Color test = new Color();
        test.setAlpha(value);

        assertEquals(test.r(), 0);
        assertEquals(test.g(), 0);
        assertEquals(test.b(), 0);
        assertEquals(test.a(), expected);
    }



    @ParameterizedTest
    @MethodSource("inverseColorArgs")
    public void testInvert(Color value, Color expected) {
        Color test = new Color(value);
        test.invert();
        assertEquals(expected, test);
    }


    @ParameterizedTest
    @MethodSource("setColorVector4fArgs")
    public void testSetColorVector4f(Vector4f pos, Color expected) {
        Color test = new Color();
        test.set(pos);
        assertEquals(test, expected);
    }

    @ParameterizedTest
    @MethodSource("setColorVector3fArgs")
    public void testSetColorVector3f(Vector3f pos, Color expected) {
        Color c = new Color();
        c.set(pos);
        assertEquals(c, expected);
    }
}

