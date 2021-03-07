// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.gestalt.di.injection;

import javax.inject.Named;
import java.util.Objects;

/**
 * The value for {@link Named#value()} is tied to the target bean and the lookup is based off of the name of the target.
 * @param <T>
 */
public class NameQualifier<T extends Named> implements Qualifier<T> {
    private final String name;

    public NameQualifier(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NameQualifier<?> that = (NameQualifier<?>) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
