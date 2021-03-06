package de.hhu.bsinfo.nio.benchmark;

import de.hhu.bsinfo.nio.benchmark.result.Combiner;
import de.hhu.bsinfo.nio.benchmark.result.Measurement;
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

    protected ThroughputWriteHandler(final SocketChannel socket, final SelectionKey key, final Combiner combiner, final int messageCount, final int messageSize) {
        super(key, combiner);
        this.socket = socket;
        remainingMessages = messageCount;
        messageBuffer = ByteBuffer.allocateDirect(messageSize);
        measurement = new ThroughputMeasurement(messageCount, messageSize);
    }

    @Override
    protected void start(final SelectionKey key) {
        LOGGER.info("Starting throughput write handler");
        startTime = System.nanoTime();
        key.interestOps(SelectionKey.OP_WRITE);
    }

    @Override
    protected Measurement getMeasurement() {
        return measurement;
    }

    @Override
    protected void handle(final SelectionKey key) {
        if (startTime == 0 || !key.isWritable()) {
            return;
        }

        try {
            socket.write(messageBuffer);
        } catch (IOException e) {
            LOGGER.error("Failed to send a message!", e);
        }

        if (!messageBuffer.hasRemaining()) {
            messageBuffer.clear();
            if (--remainingMessages <= 0) {
                final var synchronizationCounter = new SynchronizationCounter(1);
                synchronizationCounter.onZeroReached(this::finishMeasurement);
                final var synchronizationHandler = new SynchronizationHandler(socket, key, synchronizationCounter, null);
                key.attach(synchronizationHandler);
                return;
            }
        }

        key.interestOps(SelectionKey.OP_WRITE);
    }

    private void finishMeasurement() {
        try {
            measurement.setMeasuredTime(System.nanoTime() - startTime);
            close();
        } catch (IOException e) {
            LOGGER.error("Failed to close throughput write handler");
        }
    }
}
