package org.terasology.gestalt.di;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.terasology.context.AnnotationMetadata;
import org.terasology.context.AnnotationValue;
import org.terasology.context.BeanDefinition;
import org.terasology.context.annotation.Service;
import org.terasology.gestalt.di.annotation.NestedWithTestQualifier1;
import org.terasology.gestalt.di.annotation.TestQualifier1;
import org.terasology.gestalt.di.annotation.TestQualifier2;

import javax.inject.Qualifier;
import java.lang.annotation.Annotation;
import java.util.List;

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
    @Before
    public void  setup() {
        environment = new BeanEnvironment();
    }

    @Test
    public void testWithMultipleQualifiers() {
        BeanDefinition def = environment.getDefinition(AnnotationWithMultipleStereotype.class);
        AnnotationMetadata metadata = def.getAnnotationMetadata();

        List<AnnotationValue<Annotation>> annotations = metadata.getAnnotationsByStereotype(Qualifier.class);
        Assert.assertEquals(annotations.size(), 2);

        Assert.assertEquals(annotations.get(0).getAnnotationType(), TestQualifier1.class);
        Assert.assertEquals(annotations.get(1).getAnnotationType(), TestQualifier2.class);
    }

    @Test
    public void testWithNestedQualifiers() {
        BeanDefinition def = environment.getDefinition(AnnotationWithNestedStereotype.class);
        AnnotationMetadata metadata = def.getAnnotationMetadata();

        List<AnnotationValue<Annotation>> annotations = metadata.getAnnotationsByStereotype(Qualifier.class);
        Assert.assertEquals(annotations.size(), 1);

        Assert.assertEquals(annotations.get(0).getAnnotationType(), TestQualifier1.class);
    }

}
