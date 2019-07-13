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

package modules.test.components;

import org.terasology.entitysystem.core.Component;

import java.util.Objects;

public final class Sample implements Component<Sample> {
    private boolean dirty;

    private String name = "";
    private String description = "";

    public Sample() {

    }

    public Sample(Sample other) {
        copy(other);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.dirty = true;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.dirty = true;
    }

    public void copy(Sample other) {
        this.name = other.name;
        this.description = other.description;
        this.dirty = true;
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    @Override
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof Sample) {
            Sample other = (Sample) o;
            return Objects.equals(this.name, other.name) && Objects.equals(this.description, other.description);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description);
    }
}
