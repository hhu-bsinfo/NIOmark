package de.hhu.bsinfo.nio.benchmark;

import de.hhu.bsinfo.nio.benchmark.result.ThroughputMeasurement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class ThroughputWriteHandler extends BenchmarkHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThroughputWriteHandler.class);

    private final SocketChannel socket;
    private final ByteBuffer messageBuffer;
    private final ThroughputMeasurement measurement;

    private int remainingMessages;
    private long startTime;

    protected ThroughputWriteHandler(final SocketChannel socket, final SelectionKey key, final int messageCount, final int messageSize) {
        super(key);
        this.socket = socket;
        remainingMessages = messageCount;
        messageBuffer = ByteBuffer.allocateDirect(messageSize);
        measurement = new ThroughputMeasurement(messageCount, messageSize);
    }

    @Override
    protected void start(final SelectionKey key) {
        LOGGER.info("Starting throughput write handler");
        key.interestOps(SelectionKey.OP_WRITE);
        startTime = System.nanoTime();
    }

    @Override
    protected void handle(final SelectionKey key) {
        if (!key.isWritable()) {
            LOGGER.warn("Write handler called, although key is not writable");
            return;
        }

        try {
            socket.write(messageBuffer);
        } catch (IOException e) {
            LOGGER.error("Failed to send a message!", e);
        }

        if (!messageBuffer.hasRemaining()) {
            messageBuffer.clear();
            remainingMessages--;
        }

        if (remainingMessages <= 0) {
            try {
                close();
            } catch (IOException e) {
                LOGGER.error("Failed to close handler!", e);
            }
        }
    }

    @Override
    protected void close(final SelectionKey key) throws IOException {
        key.cancel();
        measurement.setMeasuredTime(System.nanoTime() - startTime);
        LOGGER.info(measurement.toString());
    }
}
