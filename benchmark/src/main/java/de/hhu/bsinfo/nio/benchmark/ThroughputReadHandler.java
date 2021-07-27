package de.hhu.bsinfo.nio.benchmark;

import de.hhu.bsinfo.nio.benchmark.result.Combiner;
import de.hhu.bsinfo.nio.benchmark.result.Measurement;
import de.hhu.bsinfo.nio.benchmark.result.ThroughputCombiner;
import de.hhu.bsinfo.nio.benchmark.result.ThroughputMeasurement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class ThroughputReadHandler extends BenchmarkHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThroughputReadHandler.class);

    private final SocketChannel socket;
    private final ByteBuffer messageBuffer;
    private final ThroughputMeasurement measurement;

    private int remainingMessages;
    private long startTime;

    protected ThroughputReadHandler(final SocketChannel socket, final SelectionKey key, final Combiner combiner, final int messageCount, final int messageSize) {
        super(key, combiner);
        this.socket = socket;
        remainingMessages = messageCount;
        messageBuffer = ByteBuffer.allocateDirect(messageSize);
        measurement = new ThroughputMeasurement(messageCount, messageSize);
    }

    @Override
    protected void start(final SelectionKey key) {
        LOGGER.info("Starting throughput read handler");
        key.interestOps(SelectionKey.OP_READ);
        startTime = System.nanoTime();
    }

    @Override
    protected Measurement getMeasurement() {
        return measurement;
    }

    @Override
    protected void handle(final SelectionKey key) {
        if (!key.isReadable()) {
            LOGGER.warn("Read handler called, although key is not readable");
            return;
        }

        try {
            socket.read(messageBuffer);
        } catch (IOException e) {
            LOGGER.error("Failed to receive a message!", e);
        }

        if (!messageBuffer.hasRemaining()) {
            messageBuffer.clear();
            remainingMessages--;
        }

        if (remainingMessages <= 0) {
            measurement.setMeasuredTime(System.nanoTime() - startTime);
            try {
                close();
            } catch (IOException e) {
                LOGGER.error("Failed to close throughput read handler");
            }
        }
    }
}
