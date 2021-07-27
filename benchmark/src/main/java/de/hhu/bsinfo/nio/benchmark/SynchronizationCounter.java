package de.hhu.bsinfo.nio.benchmark;

import java.util.concurrent.atomic.AtomicInteger;

public class SynchronizationCounter {

    private final AtomicInteger value;
    private Runnable runnable = () -> {};

    public SynchronizationCounter(final int startValue) {
        value = new AtomicInteger(startValue);
    }

    void decrement() {
        final var newValue = value.decrementAndGet();
        if (newValue == 0) {
            runnable.run();
        } else if (newValue < 0) {
            throw new IllegalStateException("Synchronization counter is below 0!");
        }
    }

    void onZeroReached(final Runnable runnable) {
        this.runnable = runnable;
    }

}
