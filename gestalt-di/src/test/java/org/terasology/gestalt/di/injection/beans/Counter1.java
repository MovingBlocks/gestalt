// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.gestalt.di.injection.beans;

import javax.inject.Inject;

public class Counter1 implements ICounter, ICounter2{
    private int count = 0;

    @Inject
    public Counter1() {

    }

    @Override
    public void add() {
        count++;
    }

    @Override
    public int getCount() {
        return count;
    }
}
