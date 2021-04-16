// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.gestalt.entitysystem.component.management;

import modules.test.components.ArrayContainingComponent;
import modules.test.components.BasicComponent;
import modules.test.components.Empty;
import modules.test.components.PublicAttributeComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.terasology.gestalt.entitysystem.component.Component;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 */
public abstract class ComponentManagerTest {

    private ComponentManager componentManager;

    @BeforeEach
    public void before() {
        componentManager = new ComponentManager(getComponentTypeFactory());
    }

    public abstract ComponentTypeFactory getComponentTypeFactory();

    @Test
    public void constructComponent() {
        BasicComponent instance = componentManager.create(BasicComponent.class);
        assertNotNull(instance);
        instance.setName("World");
        assertEquals("World", instance.getName());
    }

    @Test
    public void constructComponentGetType() {
        BasicComponent instance = componentManager.create(BasicComponent.class);
        assertNotNull(instance);
        assertEquals(BasicComponent.class, instance.getClass());
    }

    @Test
    public void constructEmptyComponent() {
        Empty instance = componentManager.create(Empty.class);
        assertNotNull(instance);
    }

    @Test
    public void emptyComponentsSingletons() {
        Empty instance = componentManager.create(Empty.class);
        Empty instance2 = componentManager.create(Empty.class);
        assertSame(instance, instance2);
    }

    @Test
    public void copyConstructor() {
        BasicComponent instance = componentManager.create(BasicComponent.class);
        instance.setName("Hello");
        BasicComponent copy = componentManager.copy(instance);
        assertEquals("Hello", copy.getName());
    }

    @Test
    public void propertyUsesHighestCommonDenominator() {
        ComponentType<MismatchedPropertiesComponent> type = componentManager.getType(MismatchedPropertiesComponent.class);
        Optional<PropertyAccessor<MismatchedPropertiesComponent, ?>> stringProperty = type.getPropertyInfo().getProperty("stringProperty");
        assertTrue(stringProperty.isPresent());
        assertEquals(CharSequence.class, stringProperty.get().getPropertyType());
    }

    @Test
    public void publicAttributesPreventSingletons() {
        ArrayContainingComponent instance = componentManager.create(ArrayContainingComponent.class);
        ArrayContainingComponent instance2 = componentManager.create(ArrayContainingComponent.class);
        assertNotSame(instance, instance2);
    }

    @Test
    public void accessProperty() {
        BasicComponent component = new BasicComponent();
        ComponentType<BasicComponent> typeInfo = componentManager.getType(BasicComponent.class);
        PropertyAccessor<BasicComponent, String> property = (PropertyAccessor<BasicComponent, String>) typeInfo.getPropertyInfo().getProperty("name").get();
        property.set(component, "Blue");
        assertEquals("Blue", property.get(component));
    }

    @Test
    public void accessPublicField() {
        PublicAttributeComponent component = new PublicAttributeComponent();
        ComponentType<PublicAttributeComponent> typeInfo = componentManager.getType(PublicAttributeComponent.class);
        PropertyAccessor<PublicAttributeComponent, String> property = (PropertyAccessor<PublicAttributeComponent, String>) typeInfo.getPropertyInfo().getProperty("name").get();
        property.set(component, "Blue");
        assertEquals("Blue", property.get(component));
    }

    public static class MismatchedPropertiesComponent implements Component<MismatchedPropertiesComponent> {
        private String stringProperty;

        public String getStringProperty() {
            return stringProperty;
        }

        public void setStringProperty(CharSequence charSequence) {
            stringProperty = charSequence.toString();
        }

        @Override
        public void copy(MismatchedPropertiesComponent other) {
            this.stringProperty = other.stringProperty;
        }
    }
}
