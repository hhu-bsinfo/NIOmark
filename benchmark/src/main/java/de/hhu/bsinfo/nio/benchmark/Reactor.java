package de.hhu.bsinfo.nio.benchmark;

import java.io.Closeable;
import java.nio.channels.Selector;

abstract class Reactor implements Runnable, Closeable {

    private final Selector selector;
    private boolean isRunning = false;

    protected Reactor(final Selector selector) {
        this.selector = selector;
    }

    @Override
    public void run() {
        isRunning = true;

        prepare(selector);

        while (isRunning) {
            react(selector);
        }
    }

    @Override
    public void close() {
        isRunning = false;
        selector.wakeup();
    }

    protected abstract void prepare(final Selector selector);

    protected abstract void react(final Selector selector);

}
