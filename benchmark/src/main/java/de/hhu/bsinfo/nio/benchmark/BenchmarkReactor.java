package de.hhu.bsinfo.nio.benchmark;

import de.hhu.bsinfo.nio.benchmark.result.Combiner;
import de.hhu.bsinfo.nio.benchmark.result.Measurement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.Selector;

public class BenchmarkReactor extends Reactor {

    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkReactor.class);

    private final Combiner combiner;

    protected BenchmarkReactor(final Selector selector, Combiner combiner) {
        super(selector);
        this.combiner = combiner;
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
            final Runnable runnable = (Runnable) key.attachment();
            if (runnable != null) {
                runnable.run();
            }
        }

        selector.selectedKeys().clear();
    }

    private void printMeasurements() {
        for (final var measurement : combiner.getMeasurements()) {
            LOGGER.info(measurement.toString());
        }

        LOGGER.info(combiner.combine().toString());
    }
}
