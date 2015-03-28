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

package org.terasology.assets.management;

import com.google.common.collect.Queues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.naming.Name;

import java.util.Deque;

/**
 * ContextManager provides the ability to set the module context in which a thread is currently running, and this context can be checked. In the asset system this
 * is used to have the current context influence the resolution of assets.
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

    /**
     * @return The Name of the module that is the current context of the thread. If there is no current context the result will be Name.EMPTY.
     */
    public static Name getCurrentContext() {
        Name context = CONTEXT_STACK.get().peek();
        return (context != null) ? context : Name.EMPTY;
    }

    /**
     * Sets the current context and returns a resource that should be closed when the context ends.
     * @param moduleId The name of the module to make the current context
     * @return A Context object that should be closed when the context ends. This will restore the preceding context if any.
     */
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
