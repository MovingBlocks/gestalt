// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.gestalt.di.injection.beans;

import org.terasology.context.annotation.Introspected;

@Introspected
public class Counter3 implements ICounter {
    @Override
    public void add() {

    }

    @Override
    public int getCount() {
        return 0;
    }
}
