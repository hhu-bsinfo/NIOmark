package de.hhu.bsinfo.nio.benchmark;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SelectionKey;

public abstract class Handler implements Runnable, Closeable {

    private final SelectionKey key;

    protected Handler(SelectionKey key) {
        this.key = key;
    }

    @Override
    public void run() {
        if (key.isValid()) {
            handle(key);
        }
    }

    @Override
    public void close() throws IOException {
        close(key);
    }

    protected abstract void handle(final SelectionKey key);

    protected abstract void close(final SelectionKey key) throws IOException;

}
