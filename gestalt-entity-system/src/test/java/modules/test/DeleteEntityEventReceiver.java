package modules.test;

import org.terasology.gestalt.entitysystem.entity.EntityRef;
import org.terasology.gestalt.entitysystem.event.Before;
import org.terasology.gestalt.entitysystem.event.EventResult;
import org.terasology.gestalt.entitysystem.event.ReceiveEvent;

import modules.test.components.Sample;
import modules.test.events.TestEvent;

public class DeleteEntityEventReceiver {

    public boolean called = false;
    public Sample component;
    public EntityRef entity;

    @ReceiveEvent
    @Before(TestEventReceiver.class)
    public EventResult testEventListener(TestEvent event, EntityRef entity, Sample sample) {
        called = true;
        component = sample;
        this.entity = entity;
        entity.delete();

        return EventResult.CONTINUE;
    }

}
