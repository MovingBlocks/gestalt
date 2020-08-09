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

package modules.test;

import org.terasology.gestalt.entitysystem.entity.EntityRef;
import org.terasology.gestalt.entitysystem.event.EventResult;
import org.terasology.gestalt.entitysystem.event.ReceiveEvent;

import modules.test.components.Sample;
import modules.test.events.TestEvent;

/**
 *
 */
public class TestEventReceiver {

    public boolean called = false;
    public Sample component;

    @ReceiveEvent
    public EventResult testEventListener(TestEvent event, EntityRef entity, Sample sample) {
        called = true;
        component = sample;
        return EventResult.COMPLETE;
    }

}
