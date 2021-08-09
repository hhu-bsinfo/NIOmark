package de.hhu.bsinfo.nio.benchmark;

import de.hhu.bsinfo.nio.benchmark.result.Combiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.Selector;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ThreadPoolBenchmarkReactor extends BenchmarkReactor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SingleThreadedBenchmarkReactor.class);

    private final Executor threadPool;

    protected ThreadPoolBenchmarkReactor(final Selector selector, final Combiner combiner, final SynchronizationCounter synchronizationCounter, final int threadPoolSize) {
        super(selector, combiner, synchronizationCounter);
        threadPool = Executors.newFixedThreadPool(threadPoolSize);
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
            key.interestOps(0);
            threadPool.execute((Runnable) key.attachment());
        }

        selector.selectedKeys().clear();
    }
}
