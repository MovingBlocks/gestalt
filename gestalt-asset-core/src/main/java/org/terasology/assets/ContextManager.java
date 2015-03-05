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

package org.terasology.assets;

import com.google.common.collect.Queues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.naming.Name;

import java.util.Deque;

/**
 * @author Immortius
 */
public final class ContextManager {

    private static final Logger logger = LoggerFactory.getLogger(ContextManager.class);
    private static final ThreadLocal<Deque<Name>> CONTEXT_STACK = new ThreadLocal<Deque<Name>>() {
        @Override
        protected Deque<Name> initialValue() {
            return Queues.newArrayDeque();
        }
    };

    private ContextManager() {
    }

    public static Name getCurrentContext() {
        Name context = CONTEXT_STACK.get().peek();
        return (context != null) ? context : Name.EMPTY;
    }

    public static Context beginContext(final Name moduleId) {
        CONTEXT_STACK.get().push(moduleId);
        return new Context() {

            @Override
            public Name getContext() {
                return moduleId;
            }

            @Override
            public void close() {
                if (!moduleId.equals(CONTEXT_STACK.get().pop())) {
                    logger.error("Module context ended out of sequence");
                }
            }
        };
    }
}
