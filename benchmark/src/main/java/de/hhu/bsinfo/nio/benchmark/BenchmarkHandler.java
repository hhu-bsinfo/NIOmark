package de.hhu.bsinfo.nio.benchmark;

import de.hhu.bsinfo.nio.benchmark.result.Combiner;
import de.hhu.bsinfo.nio.benchmark.result.Measurement;

import java.nio.channels.SelectionKey;

public abstract class BenchmarkHandler extends Handler {

    private final Combiner combiner;

    protected BenchmarkHandler(final SelectionKey key, final Combiner combiner) {
        super(key);
        this.combiner = combiner;
    }

    protected abstract void start(final SelectionKey key);

    protected abstract Measurement getMeasurement();

    @Override
    public void close(final SelectionKey key) {
        key.cancel();
        combiner.addMeasurement(getMeasurement());
    }

}
