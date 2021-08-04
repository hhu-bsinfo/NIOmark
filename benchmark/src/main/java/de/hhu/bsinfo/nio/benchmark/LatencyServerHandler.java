package de.hhu.bsinfo.nio.benchmark;

import de.hhu.bsinfo.nio.benchmark.result.Combiner;
import de.hhu.bsinfo.nio.benchmark.result.LatencyMeasurement;
import de.hhu.bsinfo.nio.benchmark.result.Measurement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class LatencyServerHandler extends BenchmarkHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(LatencyServerHandler.class);

    private final SocketChannel socket;
    private final ByteBuffer messageBuffer;
    private final LatencyMeasurement measurement;

    private int remainingMessages;
    private long startTime;

    protected LatencyServerHandler(final SocketChannel socket, final SelectionKey key, final Combiner combiner, final int messageCount, final int messageSize) {
        super(key, combiner);
        this.socket = socket;
        remainingMessages = messageCount;
        messageBuffer = ByteBuffer.allocateDirect(messageSize);
        measurement = new LatencyMeasurement(messageCount, messageSize);
    }

    @Override
    protected void start(final SelectionKey key) {
        LOGGER.info("Starting latency server handler");
        startTime = System.nanoTime();
        key.interestOps(SelectionKey.OP_WRITE);
    }

    @Override
    protected Measurement getMeasurement() {
        return measurement;
    }

    @Override
    protected void handle(final SelectionKey key) {
        if (key.isWritable()) {
            measurement.startSingleMeasurement();

            try {
                socket.write(messageBuffer);
            } catch (IOException e) {
                LOGGER.error("Failed to send a message!");
            }

            if (!messageBuffer.hasRemaining()) {
                messageBuffer.flip();
                key.interestOps(SelectionKey.OP_READ);
            }
        } else if (key.isReadable()) {
            try {
                socket.read(messageBuffer);
            } catch (IOException e) {
                LOGGER.error("Failed to receive a message!");
            }

            if (!messageBuffer.hasRemaining()) {
                measurement.stopSingleMeasurement();
                messageBuffer.flip();
                key.interestOps(SelectionKey.OP_WRITE);

                if (--remainingMessages <= 0) {
                    final var synchronizationCounter = new SynchronizationCounter(1);
                    synchronizationCounter.onZeroReached(this::finishMeasurement);
                    final var synchronizationHandler = new SynchronizationHandler(socket, key, synchronizationCounter, null);
                    key.attach(synchronizationHandler);
                }
            }
        }
    }

    private void finishMeasurement() {
        try {
            measurement.finishMeasuring(System.nanoTime() - startTime);
            close();
        } catch (IOException e) {
            LOGGER.error("Failed to close latency server handler");
        }
    }
}
