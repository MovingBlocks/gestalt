// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.gestalt.util.reflection;

import com.google.common.base.Predicate;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Immortius
 */
public class GenericsUtilTest {

    private List<?> unboundInterface;
    private List<String> boundInterface;
    private GenericClass<?> unboundClass;
    private GenericClass<String> boundClass;
    private NonGenericInterface nonGenericInterface;

    private GenericInterfaceImplementingClass<String> boundAbstractClass;

    @Test
    public void noTypeParameterForUnboundInterfaceField() throws Exception {
        Type t = GenericsUtilTest.class.getDeclaredField("unboundInterface").getGenericType();
        assertFalse(GenericsUtil.getTypeParameterBinding(t, 0).isPresent());
    }

    @Test
    public void correctParameterForBoundInterfaceField() throws Exception {
        Type t = GenericsUtilTest.class.getDeclaredField("boundInterface").getGenericType();
        Optional<Type> result = GenericsUtil.getTypeParameterBinding(t, 0);
        assertTrue(result.isPresent());
        assertEquals(String.class, result.get());
    }

    @Test
    public void noTypeParameterForUnboundClassField() throws Exception {
        Type t = GenericsUtilTest.class.getDeclaredField("unboundClass").getGenericType();
        assertFalse(GenericsUtil.getTypeParameterBinding(t, 0).isPresent());
    }

    @Test
    public void correctParameterForBoundClassField() throws Exception {
        Type t = GenericsUtilTest.class.getDeclaredField("boundClass").getGenericType();
        Optional<Type> result = GenericsUtil.getTypeParameterBinding(t, 0);
        assertTrue(result.isPresent());
        assertEquals(String.class, result.get());
    }

    @Test
    public void exceptionIfParameterIsOutOfBounds() throws Exception {
        Type t = GenericsUtilTest.class.getDeclaredField("boundInterface").getGenericType();
        assertThrows(IndexOutOfBoundsException.class, () ->
                GenericsUtil.getTypeParameterBinding(t, 1)
        );
    }

    @Test
    public void exceptionIfSuperClassIsNotParameterized() throws Exception {
        Type t = GenericsUtilTest.class.getDeclaredField("nonGenericInterface").getGenericType();
        assertThrows(IllegalArgumentException.class, () ->
                GenericsUtil.getTypeParameterBinding(t, 0)
        );
    }

    @Test
    public void boundInInheritedInterface() throws Exception {
        Optional<Type> result = GenericsUtil.getTypeParameterBindingForInheritedClass(BoundInterface.class, Predicate.class, 0);
        assertTrue(result.isPresent());
        assertEquals(String.class, result.get());
    }

    @Test
    public void boundInInheritedClass() throws Exception {
        Optional<Type> result = GenericsUtil.getTypeParameterBindingForInheritedClass(BoundClass.class, Predicate.class, 0);
        assertTrue(result.isPresent());
        assertEquals(String.class, result.get());
    }

    @Test
    public void boundOneStepRemoved() throws Exception {
        Type t = GenericsUtilTest.class.getDeclaredField("boundAbstractClass").getGenericType();
        Optional<Type> result = GenericsUtil.getTypeParameterBindingForInheritedClass(t, Predicate.class, 0);
        assertTrue(result.isPresent());
        assertEquals(String.class, result.get());
    }

    public interface NonGenericInterface {

    }

    public interface BoundInterface extends Predicate<String> {

    }

    public abstract static class BoundClass implements Predicate<String> {

    }

    public static class GenericInterfaceImplementingClass<T> implements Predicate<T> {

        @Override
        public boolean apply(T input) {
            return false;
        }
    }

    public static class GenericClass<T> {

    }
}
