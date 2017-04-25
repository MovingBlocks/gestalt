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

package org.terasology.entitysystem.index;

import com.google.common.base.Preconditions;
import org.terasology.entitysystem.core.Component;
import org.terasology.entitysystem.core.EntityManager;
import org.terasology.entitysystem.transaction.TransactionManager;
import org.terasology.util.Varargs;

import java.util.Collections;
import java.util.Set;

/**
 *
 */
public final class ComponentIndexes {

    private ComponentIndexes() {
    }

    public static Index createComponentIndex(TransactionManager transactionManager, EntityManager entityManager, Class<? extends Component> firstComponentType, Class<? extends Component> ... additionalComponentTypes) {
        return createComponentIndex(transactionManager, entityManager, Varargs.combineToSet(firstComponentType, additionalComponentTypes));
    }

    public static Index createComponentIndex(TransactionManager transactionManager, EntityManager entityManager, Set<Class<? extends Component>> componentTypes) {
        Preconditions.checkArgument(componentTypes != null && !componentTypes.isEmpty());
        return new GenericIndex(transactionManager, entityManager, (x) -> !Collections.disjoint(x, componentTypes), (x) -> x.keySet().containsAll(componentTypes));
    }
}
