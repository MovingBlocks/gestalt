package org.terasology.gestalt.di.injection.beans;

import org.terasology.context.annotation.Introspected;

@Introspected
public class Counter1 implements ICounter , ICounter2{
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
