package de.hhu.bsinfo.nio.benchmark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class Acceptor implements Runnable, Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Acceptor.class);

    private final Benchmark benchmark;
    private final ServerSocketChannel serverSocketChannel;
    private final SelectionKey key;

    public Acceptor(final Benchmark benchmark, final ServerSocketChannel serverSocketChannel, final SelectionKey key) {
        this.benchmark = benchmark;
        this.serverSocketChannel = serverSocketChannel;
        this.key = key;
    }

    @Override
    public void run() {
        LOGGER.info("Handling incoming connection request");
        SocketChannel socketChannel;

        try {
            socketChannel = serverSocketChannel.accept();
            benchmark.addIncomingConnection(socketChannel);
        } catch (IOException e) {
            LOGGER.error("Failed to accept connection request", e);
            return;
        }

        try {
            LOGGER.info("Accepted connection request from [{}]", socketChannel.getRemoteAddress());
        } catch (IOException e) {
            LOGGER.info("Accepted connection request");
        }
    }

    @Override
    public void close() throws IOException {
        LOGGER.info("Closing acceptor");
        key.cancel();
        serverSocketChannel.close();
    }
}
