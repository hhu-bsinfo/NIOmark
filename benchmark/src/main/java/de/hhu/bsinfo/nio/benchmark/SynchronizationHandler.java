package de.hhu.bsinfo.nio.benchmark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class SynchronizationHandler extends Handler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionHandler.class);
    private static final String SYNC_MESSAGE = "SYNC";

    private final ByteBuffer sendBuffer = ByteBuffer.allocateDirect(SYNC_MESSAGE.getBytes(StandardCharsets.UTF_8).length);
    private final ByteBuffer receiveBuffer = ByteBuffer.allocateDirect(SYNC_MESSAGE.getBytes(StandardCharsets.UTF_8).length);

    private final SocketChannel socketChannel;
    private final SynchronizationCounter synchronizationCounter;
    private final BenchmarkHandler benchmarkHandler;

    protected SynchronizationHandler(final SocketChannel socketChannel, final SelectionKey key, final SynchronizationCounter synchronizationCounter, final BenchmarkHandler benchmarkHandler) {
        super(key);
        this.socketChannel = socketChannel;
        this.synchronizationCounter = synchronizationCounter;
        this.benchmarkHandler = benchmarkHandler;
        key.interestOps(SelectionKey.OP_WRITE);
    }

    @Override
    protected void handle(final SelectionKey key) {
        if (key.isWritable()) {
            handleWrite(key);
        } else if (key.isReadable()) {
            handleRead(key);
        }
    }

    @Override
    protected void close(final SelectionKey key) throws IOException {
        LOGGER.info("Closing synchronization handler");
        key.attach(benchmarkHandler);
        synchronizationCounter.decrement();
    }

    private void handleWrite(final SelectionKey key) {
        try {
            LOGGER.info("Sending synchronization message to [{}]", socketChannel.getRemoteAddress());
        } catch (IOException e) {
            LOGGER.info("Sending synchronization message");
        }

        try {
            final var written = socketChannel.write(sendBuffer);
            if (written == -1) {
                LOGGER.error("Failed to write synchronization message");
                key.cancel();
            }

            if (!sendBuffer.hasRemaining()) {
                key.interestOps(SelectionKey.OP_READ);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleRead(final SelectionKey key) {
        try {
            LOGGER.info("Receiving synchronization message from [{}]", socketChannel.getRemoteAddress());
        } catch (IOException e) {
            LOGGER.info("Receiving synchronization message");
        }

        try {
            final var read = socketChannel.read(receiveBuffer);
            if (read == -1) {
                LOGGER.error("Failed to read synchronization message");
                key.cancel();
            }

            if (!sendBuffer.hasRemaining()) {
                close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
