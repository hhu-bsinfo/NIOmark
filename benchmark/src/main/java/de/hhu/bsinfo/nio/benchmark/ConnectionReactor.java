package de.hhu.bsinfo.nio.benchmark;

import de.hhu.bsinfo.nio.benchmark.result.Combiner;
import de.hhu.bsinfo.nio.benchmark.result.ThroughputCombiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Set;

public class ConnectionReactor extends Reactor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionReactor.class);

    private final Combiner combiner;
    private final BenchmarkConfiguration configuration;
    private final SynchronizationCounter synchronizationCounter;
    private final Set<InetSocketAddress> outgoingConnections;
    private final Set<InetSocketAddress> remainingConnections;
    private final Set<InetSocketAddress> establishedConnections;

    private int remainingIncomingConnections;
    private boolean outgoingConnectionsFinished = false;
    private boolean incomingConnectionsFinished = false;

    public ConnectionReactor(final Selector selector, final Combiner combiner, final BenchmarkConfiguration configuration, final SynchronizationCounter synchronizationCounter, final Set<InetSocketAddress> outgoingConnections, final int incomingConnections) {
        super(selector);
        this.combiner = combiner;
        this.configuration = configuration;
        this.synchronizationCounter = synchronizationCounter;
        this.outgoingConnections = Set.copyOf(outgoingConnections);
        this.remainingConnections = new HashSet<>(outgoingConnections);
        this.establishedConnections = new HashSet<>();
        this.remainingIncomingConnections = incomingConnections;
    }

    @Override
    public void react(final Selector selector) {
        for (final var address : remainingConnections) {
            try {
                final var socketChannel = SocketChannel.open();
                socketChannel.configureBlocking(false);

                final var socketKey = socketChannel.register(selector, SelectionKey.OP_CONNECT);
                final var handler = new ConnectionHandler(this, combiner, configuration, synchronizationCounter, socketChannel, socketKey, address);
                socketKey.attach(handler);

                socketChannel.connect(address);
            } catch (IOException e) {
                LOGGER.error("Failed to initiate connection establishment to [{}]", address);
            }
        }

        remainingConnections.clear();

        handleConnectionEstablishment(selector);
        if (incomingConnectionsFinished && outgoingConnectionsFinished) {
            close();
            return;
        }

        try {
            selector.select();
        } catch (IOException e) {
            LOGGER.error("Failed to select keys", e);
            return;
        }

        for (final var key : selector.selectedKeys()) {
            final Runnable runnable = (Runnable) key.attachment();

            if (runnable instanceof ConnectionHandler || runnable instanceof Acceptor) {
                runnable.run();
            }
        }

        selector.selectedKeys().clear();
    }

    private void handleConnectionEstablishment(final Selector selector) {
        if (!incomingConnectionsFinished) {
            LOGGER.info("Remaining incoming connections: [{}]", remainingIncomingConnections);
            if (remainingIncomingConnections == 0) {
                LOGGER.info("All incoming connections are established");
                incomingConnectionsFinished = true;

                for (final var key : selector.keys()) {
                    if (key.attachment() != null && key.attachment().getClass() == Acceptor.class) {
                        try {
                            ((Acceptor) key.attachment()).close();
                        } catch (IOException e) {
                            LOGGER.error("Failed to close acceptor", e);
                        }
                    }
                }
            }
        }

        if (!outgoingConnectionsFinished) {
            LOGGER.info("Remaining outgoing connections: [{}]", outgoingConnections.size() - establishedConnections.size());
            if (establishedConnections.equals(outgoingConnections)) {
                LOGGER.info("All outgoing connections are established");
                outgoingConnectionsFinished = true;
            }
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            LOGGER.error("Thread has been interrupted unexpectedly", e);
        }
    }

    void addRemainingConnection(final InetSocketAddress address) {
        remainingConnections.add(address);
    }

    void addEstablishedConnection(final InetSocketAddress address) {
        establishedConnections.add(address);
    }

    void addIncomingConnection() {
        remainingIncomingConnections--;
    }
}
