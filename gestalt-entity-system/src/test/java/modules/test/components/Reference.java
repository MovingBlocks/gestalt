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

import org.terasology.entitysystem.component.Component;
import org.terasology.entitysystem.entity.EntityRef;
import org.terasology.entitysystem.entity.NullEntityRef;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 *
 */
public final class Reference implements Component<Reference> {

    private boolean dirty;

    private EntityRef reference = NullEntityRef.get();
    private List<EntityRef> references = Lists.newArrayList();

    public Reference() {

    }

    public Reference(Reference other) {
        copy(other);
    }

    public EntityRef getReference() {
        return reference;
    }

    public void setReference(EntityRef ref) {
        this.reference = ref;
        this.dirty = true;
    }

    public List<EntityRef> getReferences() {
        return Collections.unmodifiableList(references);
    }

    public void setReferences(List<EntityRef> references) {
        this.references.clear();
        this.references.addAll(references);
        this.dirty = true;
    }

    public void copy(Reference other) {
        setReferences(other.references);
        this.reference = other.reference;
        this.dirty = true;
    }

    @Override
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof Reference) {
            Reference other = (Reference) o;
            return Objects.equals(this.reference, other.reference) && Objects.equals(this.references, other.references);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(reference, references);
    }
}
