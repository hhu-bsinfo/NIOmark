package de.hhu.bsinfo.nio.benchmark;

import de.hhu.bsinfo.nio.benchmark.result.Combiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.Selector;

public abstract class BenchmarkReactor extends Reactor {

    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkReactor.class);

    private final Combiner combiner;

    protected BenchmarkReactor(final Selector selector, final Combiner combiner, final SynchronizationCounter synchronizationCounter) {
        super(selector);
        this.combiner = combiner;
        synchronizationCounter.onZeroReached(() -> startMeasurements(selector));
    }


    void startMeasurements(final Selector selector) {
        LOGGER.info("Synchronization finished -> Starting benchmark");

        for (final var key : selector.keys()) {
            if (key.attachment() instanceof BenchmarkHandler) {
                ((BenchmarkHandler) key.attachment()).start(key);
            }
        }
    }

    protected void printMeasurements() {
        for (final var measurement : combiner.getMeasurements()) {
            LOGGER.info(measurement.toString());
        }

        LOGGER.info(combiner.combine().toString());
    }
}
