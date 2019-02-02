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

package org.terasology.util.reflection;

import com.google.common.base.Predicate;

import org.junit.Test;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

    @Test(expected = IndexOutOfBoundsException.class)
    public void exceptionIfParameterIsOutOfBounds() throws Exception {
        Type t = GenericsUtilTest.class.getDeclaredField("boundInterface").getGenericType();
        GenericsUtil.getTypeParameterBinding(t, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void exceptionIfSuperClassIsNotParameterized() throws Exception {
        Type t = GenericsUtilTest.class.getDeclaredField("nonGenericInterface").getGenericType();
        GenericsUtil.getTypeParameterBinding(t, 0);
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
