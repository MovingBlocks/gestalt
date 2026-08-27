package org.terasology.gestalt.di;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.terasology.context.AnnotationMetadata;
import org.terasology.context.AnnotationValue;
import org.terasology.context.BeanDefinition;
import org.terasology.context.annotation.Service;
import org.terasology.gestalt.di.annotation.NestedWithTestQualifier1;
import org.terasology.gestalt.di.annotation.TestQualifier1;
import org.terasology.gestalt.di.annotation.TestQualifier2;
import org.terasology.gestalt.di.annotation.WithProperties;

import javax.inject.Qualifier;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

public class AnnotationTest {
    private BeanEnvironment environment ;

    @Service
    @TestQualifier1
    @TestQualifier2
    public static class AnnotationWithMultipleStereotype {
    }

    @Service
    @NestedWithTestQualifier1
    public static class AnnotationWithNestedStereotype {

    }
    @BeforeEach
    public void  setup() {
        environment = new BeanEnvironment();
    }

    @Test
    public void testWithMultipleQualifiers() {
        BeanDefinition def = environment.getDefinition(AnnotationWithMultipleStereotype.class);
        AnnotationMetadata metadata = def.getAnnotationMetadata();

        List<AnnotationValue<Annotation>> annotations = metadata.getAnnotationsByStereotype(Qualifier.class);
        Assertions.assertEquals(annotations.size(), 2);

        Assertions.assertEquals(annotations.get(0).getAnnotationType(), TestQualifier1.class);
        Assertions.assertEquals(annotations.get(1).getAnnotationType(), TestQualifier2.class);
    }

    @Test
    public void testWithNestedQualifiers() {
        BeanDefinition def = environment.getDefinition(AnnotationWithNestedStereotype.class);
        AnnotationMetadata metadata = def.getAnnotationMetadata();

        List<AnnotationValue<Annotation>> annotations = metadata.getAnnotationsByStereotype(Qualifier.class);
        Assertions.assertEquals(annotations.size(), 1);

        Assertions.assertEquals(annotations.get(0).getAnnotationType(), TestQualifier1.class);
    }

    @Test
    public void testQualifier1Property() {
        BeanDefinition def = environment.getDefinition(AnnotationWithMultipleStereotype.class);
        AnnotationMetadata metadata = def.getAnnotationMetadata();

        List<AnnotationValue<TestQualifier2>> annotations = metadata.findAnnotations(TestQualifier2.class);
        Assertions.assertEquals(annotations.size(), 1);
        AnnotationValue[] withProperties = annotations.get(0).getAnnotation(WithProperties.class);
        Assertions.assertEquals(withProperties.length, 1);

        AnnotationValue value = withProperties[0];

        Optional<String> p1 = value.stringValue("p1");
        Assertions.assertTrue(p1.isPresent());
        Assertions.assertEquals(p1.get(), "Hello");

        OptionalDouble p2 = value.doubleValue("p4");
        Assertions.assertTrue(p2.isPresent());
        Assertions.assertEquals(p2.getAsDouble(), 5.0,0.001);
    }

}
