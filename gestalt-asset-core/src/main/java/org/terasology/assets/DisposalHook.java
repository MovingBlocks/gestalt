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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.reflect.Modifier;
import java.util.Optional;

/**
 * DisposalHook holds the action to occur when an asset is disposed. This is handled outside of the asset class itself to allow disposal to occur after the asset has been
 * garbage collected via a disposed reference queue mechanism in AssetType.
 */
class DisposalHook {

    private static final Logger logger = LoggerFactory.getLogger(DisposalHook.class);
    private volatile Runnable disposeAction;

    synchronized void dispose() {
        if (disposeAction != null) {
            disposeAction.run();
        }
        disposeAction = null;
    }

    public void setDisposeAction(@Nullable Runnable disposeAction) {
        if (disposeAction != null) {
            Class<? extends Runnable> actionType = disposeAction.getClass();
            if ((actionType.isLocalClass() || actionType.isAnonymousClass() || actionType.isMemberClass()) && !Modifier.isStatic(actionType.getModifiers())) {
                logger.warn("Non-static anonymous or member class should not be registered as the disposal hook - this will block garbage collection enqueuing for disposal");
            }
        }
        this.disposeAction = disposeAction;
    }
}
