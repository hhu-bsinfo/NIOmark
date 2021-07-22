package de.hhu.bsinfo.nio.benchmark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.Selector;

public class BenchmarkReactor extends Reactor {

    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkReactor.class);

    protected BenchmarkReactor(final Selector selector) {
        super(selector);
    }

    @Override
    protected void prepare(Selector selector) {
        for (final var key : selector.keys()) {
            final var handler = key.attachment();
            if (handler instanceof BenchmarkHandler) {
                ((BenchmarkHandler) handler).start();
            }
        }
    }

    @Override
    protected void react(final Selector selector) {
        if (selector.keys().isEmpty()) {
            close();
        }

        try {
            selector.select();
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
}
