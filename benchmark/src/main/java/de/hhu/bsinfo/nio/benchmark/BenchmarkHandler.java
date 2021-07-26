package de.hhu.bsinfo.nio.benchmark;

import java.nio.channels.SelectionKey;

public abstract class BenchmarkHandler extends Handler {

    protected BenchmarkHandler(final SelectionKey key) {
        super(key);
    }

    protected abstract void start(final SelectionKey key);

}
