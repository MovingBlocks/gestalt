package org.terasology.gestalt.di.injection.beans;

import org.terasology.context.annotation.Introspected;

import javax.inject.Inject;
import javax.inject.Named;

@Introspected
public class CounterTester {
    private final ICounter c1;
    private final ICounter c2;

    @Inject
    public CounterTester(@Named("Counter1") ICounter counter1,@Named("Counter2") ICounter counter2) {
        this.c1 = counter1;
        this.c2 = counter2;
    }

    public void addToCounter1() {
        c1.add();
    }

    public void addToCounter2() {
        c2.add();
    }

    public int getCounter1Count() {
        return c1.getCount();
    }

    public int getCounter2Count() {
        return c2.getCount();
    }
}
