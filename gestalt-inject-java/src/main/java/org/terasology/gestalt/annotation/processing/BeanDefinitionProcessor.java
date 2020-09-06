package org.terasology.gestalt.annotation.processing;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import java.util.Set;

public class BeanDefinitionProcessor extends AbstractProcessor {
    private static final String EVENT_INTERFACE = "org.terasology.gestalt.entitysystem.event.Event";

    private static final String RECEIVE_EVENT_TYPE = "org.terasology.gestalt.entitysystem.event.ReceiveEvent";

    private static final String[] TARGET_ANNOTATIONS = new String[]{
        "javax.inject.Inject",
        "javax.inject.Qualifier",
        "javax.inject.Singleton",
        RECEIVE_EVENT_TYPE
    };

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {

        return false;
    }
}
