package de.hhu.bsinfo.nio.benchmark;

import de.hhu.bsinfo.nio.benchmark.result.Combiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.Selector;

public class SingleThreadedBenchmarkReactor extends BenchmarkReactor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SingleThreadedBenchmarkReactor.class);

    protected SingleThreadedBenchmarkReactor(final Selector selector, final Combiner combiner, final SynchronizationCounter synchronizationCounter) {
        super(selector, combiner, synchronizationCounter);
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
}
