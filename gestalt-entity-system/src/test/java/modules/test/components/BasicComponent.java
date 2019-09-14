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

import com.google.common.collect.Lists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitysystem.component.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 *
 */
public final class BasicComponent implements Component<BasicComponent> {
    private static Logger logger = LoggerFactory.getLogger(BasicComponent.class);
    private boolean dirty;

    private String name = "";
    private String description = "";
    private int count = 0;
    private List<String> friends = Lists.newArrayList();

    public BasicComponent() {

    }

    public BasicComponent(BasicComponent other) {
        copy(other);
        logger.info("Copy constructor called");
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

    public List<String> getFriends() {
        return Collections.unmodifiableList(friends);
    }

    public void setFriends(List<String> friends) {
        this.friends = friends;
        this.dirty = true;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
        this.dirty = true;
    }

    public void copy(BasicComponent other) {
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
        if (o instanceof BasicComponent) {
            BasicComponent other = (BasicComponent) o;
            return Objects.equals(this.name, other.name) && Objects.equals(this.description, other.description);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description);
    }
}
