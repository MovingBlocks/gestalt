// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.gestalt.di.injection.beans;

import org.terasology.context.annotation.Introspected;

import javax.inject.Inject;

public class Counter2 implements ICounter {
    @Inject
    public Counter2() {

    }
    private int count = 0;

    @Override
    public void add() {
        count++;
    }

    @Override
    public int getCount() {
        return count;
    }
}