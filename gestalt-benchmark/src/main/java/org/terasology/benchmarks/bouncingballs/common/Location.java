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

package org.terasology.benchmarks.bouncingballs.common;

import org.terasology.entitysystem.core.Component;

public class Location implements Component<Location> {

    private boolean dirty;
    private float x;
    private float y;
    private float z;

    @Override
    public boolean isDirty() {
        return dirty;
    }

    @Override
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    @Override
    public void copy(Location other) {
        this.dirty = true;
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
        this.dirty = true;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
        this.dirty = true;
    }

    public float getZ() {
        return z;
    }

    public void setZ(float z) {
        this.z = z;
        this.dirty = true;
    }
}

