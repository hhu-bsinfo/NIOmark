package de.hhu.bsinfo.nio.benchmark;

import de.hhu.bsinfo.nio.benchmark.result.Measurement;

import java.nio.channels.SelectionKey;

public abstract class BenchmarkHandler extends Handler {

    protected BenchmarkHandler(SelectionKey key) {
        super(key);
    }

    protected abstract void start();

}
