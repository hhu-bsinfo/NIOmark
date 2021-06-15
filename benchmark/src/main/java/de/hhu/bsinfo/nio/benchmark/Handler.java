package de.hhu.bsinfo.nio.benchmark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class Handler implements Runnable, Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Handler.class);

    private final Benchmark benchmark;
    private final SocketChannel socketChannel;
    private final SelectionKey key;
    private final InetSocketAddress remoteAddress;

    public Handler(Benchmark benchmark, SocketChannel socketChannel, SelectionKey key, InetSocketAddress remoteAddress) {
        this.benchmark = benchmark;
        this.socketChannel = socketChannel;
        this.key = key;
        this.remoteAddress = remoteAddress;
    }

    public Handler(Benchmark benchmark, SocketChannel socketChannel, SelectionKey key) {
        this.benchmark = benchmark;
        this.socketChannel = socketChannel;
        this.key = key;
        this.remoteAddress = null;
    }

    @Override
    public void run() {
        if (!key.isValid()) {
            return;
        }

        if (key.isConnectable()) {
            try {
                LOGGER.info("Finishing connection establishment to [{}]", socketChannel.getRemoteAddress());
            } catch (IOException e) {
                LOGGER.info("Handling connection event");
            }

            try {
                socketChannel.finishConnect();
                benchmark.addEstablishedConnection(remoteAddress);
                key.interestOps(SelectionKey.OP_WRITE);
            } catch (IOException e) {
                LOGGER.error("Failed to establish connection");
                key.cancel();
                benchmark.addRemainingConnection(remoteAddress);
                return;
            }

            try {
                LOGGER.info("Established connection from [{}] to [{}]", socketChannel.getLocalAddress(), socketChannel.getRemoteAddress());
            } catch (IOException e) {
                LOGGER.info("Established connection");
            }
        }
    }

    @Override
    public void close() throws IOException {
        try {
            LOGGER.info("Closing handler for connection from [{}] to [{}]", socketChannel.getLocalAddress(), socketChannel.getRemoteAddress());
        } catch (IOException e) {
            LOGGER.info("Closing handler");
        }

        key.cancel();
        socketChannel.close();
    }
}
