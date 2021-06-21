package de.hhu.bsinfo.nio.benchmark;

import java.io.Closeable;
import java.nio.channels.Selector;

abstract class Reactor implements Runnable, Closeable {

    private final Selector selector;
    private boolean isRunning = false;

    protected Reactor(Selector selector) {
        this.selector = selector;
    }

    @Override
    public void run() {
        isRunning = true;

        while (isRunning) {
            react(selector);
        }
    }

    @Override
    public void close() {
        isRunning = false;
        selector.wakeup();
    }

    protected abstract void react(final Selector selector);

}
