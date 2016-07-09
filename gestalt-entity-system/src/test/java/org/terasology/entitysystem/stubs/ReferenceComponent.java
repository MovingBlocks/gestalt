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

package org.terasology.entitysystem.stubs;

import org.terasology.entitysystem.entity.Component;
import org.terasology.entitysystem.entity.EntityRef;

import java.util.List;

/**
 *
 */
public interface ReferenceComponent extends Component {

    EntityRef getReference();

    List<EntityRef> getReferences();

    void setReference(EntityRef ref);

    void setReferences(List<EntityRef> references);
}
