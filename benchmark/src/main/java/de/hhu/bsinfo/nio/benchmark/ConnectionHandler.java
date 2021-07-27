package de.hhu.bsinfo.nio.benchmark;

import de.hhu.bsinfo.nio.benchmark.result.Combiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class ConnectionHandler extends Handler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionHandler.class);

    private final ConnectionReactor reactor;
    private final Combiner combiner;
    private final BenchmarkConfiguration configuration;
    private final SynchronizationCounter synchronizationCounter;
    private final SocketChannel socketChannel;
    private final InetSocketAddress remoteAddress;

    public ConnectionHandler(final ConnectionReactor reactor, final Combiner combiner, final BenchmarkConfiguration configuration, final SynchronizationCounter synchronizationCounter, final SocketChannel socketChannel, final SelectionKey key, final InetSocketAddress remoteAddress) {
        super(key);
        this.reactor = reactor;
        this.combiner = combiner;
        this.configuration = configuration;
        this.synchronizationCounter = synchronizationCounter;
        this.socketChannel = socketChannel;
        this.remoteAddress = remoteAddress;
    }

    @Override
    protected void handle(final SelectionKey key) {
        if (!key.isConnectable()) {
            return;
        }

        try {
            LOGGER.info("Finishing connection establishment to [{}]", socketChannel.getRemoteAddress());
        } catch (IOException e) {
            LOGGER.info("Handling connection event");
        }

        try {
            socketChannel.finishConnect();
        } catch (IOException e) {
            LOGGER.error("Failed to establish connection");
            key.cancel();
            reactor.addRemainingConnection(remoteAddress);
            return;
        }

        try {
            LOGGER.info("Established connection from [{}] to [{}]", socketChannel.getLocalAddress(), socketChannel.getRemoteAddress());
        } catch (IOException e) {
            LOGGER.info("Established connection");
        }

        final var benchmarkHandler = new ThroughputWriteHandler(socketChannel, key, combiner, configuration.getOperationCount(), configuration.getOperationSize());
        final var synchronizationHandler = new SynchronizationHandler(socketChannel, key, synchronizationCounter, benchmarkHandler);
        key.attach(synchronizationHandler);
        reactor.addEstablishedConnection(remoteAddress);

        try {
            close();
        } catch (IOException e) {
            LOGGER.error("Failed to close connection handler");
        }
    }

    @Override
    protected void close(final SelectionKey key) {
        try {
            LOGGER.info("Closing connection handler for connection from [{}] to [{}]", socketChannel.getLocalAddress(), socketChannel.getRemoteAddress());
        } catch (IOException e) {
            LOGGER.info("Closing connection handler");
        }
    }
}
