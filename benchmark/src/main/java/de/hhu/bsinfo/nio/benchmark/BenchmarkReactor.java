package de.hhu.bsinfo.nio.benchmark;

import de.hhu.bsinfo.nio.benchmark.result.Combiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.Selector;

public class BenchmarkReactor extends Reactor {

    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkReactor.class);

    private final Combiner combiner;

    protected BenchmarkReactor(final Selector selector, final Combiner combiner, final SynchronizationCounter synchronizationCounter) {
        super(selector);
        this.combiner = combiner;

        synchronizationCounter.onZeroReached(() -> startMeasurements(selector));
    }

    @Override
    protected void react(final Selector selector) {
        if (selector.keys().isEmpty()) {
            close();
            printMeasurements();
        }

        try {
            selector.selectNow();
        } catch (IOException e) {
            LOGGER.error("Failed to select keys", e);
            return;
        }

        for (final var key : selector.selectedKeys()) {
            ((Runnable) key.attachment()).run();
        }

        selector.selectedKeys().clear();
    }

    void startMeasurements(final Selector selector) {
        LOGGER.info("Synchronization finished -> Starting benchmark");

        for (final var key : selector.keys()) {
            if (key.attachment() instanceof BenchmarkHandler) {
                ((BenchmarkHandler) key.attachment()).start(key);
            }
        }
    }

    private void printMeasurements() {
        for (final var measurement : combiner.getMeasurements()) {
            LOGGER.info(measurement.toString());
        }

        LOGGER.info(combiner.combine().toString());
    }
}
