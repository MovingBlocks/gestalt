/*
 * Copyright 2015 MovingBlocks
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

import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Immortius
 */
public class ClassFactoryTest {

    @Test
    public void constructWithDefaultConstructor() {
        ClassFactory classFactory = new SimpleClassFactory();
        Optional<TargetInterface> result = classFactory.instantiateClass(SimpleClass.class);
        assertTrue(result.isPresent());
        assertTrue(result.get() instanceof SimpleClass);
    }

    @Test
    public void constructWithUnavailableConstructorAndDefault() {
        ClassFactory classFactory = new SimpleClassFactory();
        Optional<TargetInterface> result = classFactory.instantiateClass(MultiConstructor.class);
        assertTrue(result.isPresent());
        assertTrue(result.get() instanceof MultiConstructor);
        assertEquals("", ((MultiConstructor) result.get()).value);
    }

    @Test
    public void constructWithAvailableValueConstructor() {
        ClassFactory classFactory = new SimpleClassFactory(new ParameterProvider() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> Optional<T> get(Class<T> type) {
                if (String.class.equals(type)) {
                    return (Optional<T>) Optional.of("Value");
                }
                return Optional.empty();
            }
        });
        Optional<TargetInterface> result = classFactory.instantiateClass(MultiConstructor.class);
        assertTrue(result.isPresent());
        assertTrue(result.get() instanceof MultiConstructor);
        assertEquals("Value", ((MultiConstructor) result.get()).value);
    }

    @Test
    public void failWithNoValidConstructor() {
        ClassFactory classFactory = new SimpleClassFactory();
        Optional<TargetInterface> result = classFactory.instantiateClass(StringRequiredConstructor.class);
        assertFalse(result.isPresent());
    }

    @Test
    public void fillMissingOptionalParamsWithEmpty() {
        ClassFactory classFactory = new SimpleClassFactory();
        Optional<TargetInterface> result = classFactory.instantiateClass(OptionalValueConstructor.class);
        assertTrue(result.isPresent());
        OptionalValueConstructor item = (OptionalValueConstructor) result.get();
        assertFalse(item.value.isPresent());
    }

    @Test
    public void fillAvailableOptionalParams() {
        ClassFactory classFactory = new SimpleClassFactory(new ParameterProvider() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> Optional<T> get(Class<T> type) {
                if (String.class.equals(type)) {
                    return (Optional<T>) Optional.of("Value");
                }
                return Optional.empty();
            }
        });
        Optional<TargetInterface> result = classFactory.instantiateClass(OptionalValueConstructor.class);
        assertTrue(result.isPresent());
        OptionalValueConstructor item = (OptionalValueConstructor) result.get();
        assertTrue(item.value.isPresent());
        assertEquals("Value", item.value.get());
    }

    public interface TargetInterface {
    }

    public static class SimpleClass implements TargetInterface {

    }

    public static class MultiConstructor implements TargetInterface {
        public String value = "";

        public MultiConstructor() {

        }

        public MultiConstructor(String value) {
            this.value = value;
        }
    }

    public static class StringRequiredConstructor implements TargetInterface {
        public String value = "";

        public StringRequiredConstructor(String value) {
            this.value = value;
        }
    }


    public static class OptionalValueConstructor implements TargetInterface {
        public Optional<String> value = Optional.empty();

        public OptionalValueConstructor(Optional<String> value) {
            this.value = value;
        }
    }
}
