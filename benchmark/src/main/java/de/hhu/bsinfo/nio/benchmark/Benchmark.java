package de.hhu.bsinfo.nio.benchmark;

import de.hhu.bsinfo.nio.generated.BuildConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class Benchmark implements Runnable, Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Benchmark.class);

    private final Selector selector;
    private final Set<InetSocketAddress> outgoingConnections;
    private final Set<InetSocketAddress> remainingConnections;
    private final Set<InetSocketAddress> establishedConnections;
    private int remainingIncomingConnections;

    private boolean isRunning = false;
    private boolean outgoingConnectionsFinished = false;
    private boolean incomingConnectionsFinished = false;

    private Benchmark(final Selector selector, final Set<InetSocketAddress> outgoingConnections, final int incomingConnections) {
        this.selector = selector;
        this.outgoingConnections = Set.copyOf(outgoingConnections);
        this.remainingConnections = new HashSet<>(outgoingConnections);
        this.establishedConnections = new HashSet<>();
        this.remainingIncomingConnections = incomingConnections;
    }

    public static Benchmark createBenchmark(final InetSocketAddress localAddress, final Set<InetSocketAddress> outgoingConnections, final int incomingConnections) throws IOException {
        LOGGER.info("Creating benchmark instance (localAddress: [{}], outgoingConnections: {})", localAddress, outgoingConnections);

        final var selector = Selector.open();
        final var serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(localAddress);

        final var benchmark = new Benchmark(selector, outgoingConnections, incomingConnections);
        final var serverKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        final var acceptor = new Acceptor(benchmark, serverSocketChannel, serverKey);
        serverKey.attach(acceptor);

        return benchmark;
    }

    @Override
    public void run() {
        isRunning = true;
        LOGGER.info("Starting NIO reactor loop");

        while(isRunning && !selector.keys().isEmpty()) {
            for (final var address : remainingConnections) {
                try {
                    final var socketChannel = SocketChannel.open();
                    socketChannel.configureBlocking(false);

                    final var socketKey = socketChannel.register(selector, SelectionKey.OP_CONNECT);
                    final var handler = new Handler(this, socketChannel, socketKey, address);
                    socketKey.attach(handler);

                    socketChannel.connect(address);
                } catch (IOException e) {
                    LOGGER.error("Failed to initiate connection establishment to [{}]", address);
                }
            }

            remainingConnections.clear();

            try {
                selector.select();
            } catch (IOException e) {
                LOGGER.error("Failed to select keys", e);
                return;
            }

            for (final SelectionKey key : selector.selectedKeys()) {
                final Runnable runnable = (Runnable) key.attachment();
                if (runnable != null) {
                    runnable.run();
                }
            }

            if (!incomingConnectionsFinished || !outgoingConnectionsFinished) {
                handleConnectionEstablishment();
            }

            selector.selectedKeys().clear();
        }

        LOGGER.info("Finished NIO reactor loop");
    }

    private void handleConnectionEstablishment() {
        if (!incomingConnectionsFinished) {
            LOGGER.info("Remaining incoming connections: [{}]", remainingIncomingConnections);
            if (remainingIncomingConnections == 0) {
                LOGGER.info("All incoming connections are established");
                incomingConnectionsFinished = true;

                for (final var key : selector.keys()) {
                    if (key.attachment().getClass() == Acceptor.class) {
                        try {
                            key.cancel();
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

    @Override
    public void close() throws IOException {
        LOGGER.info("Closing benchmark");
        for (final var key : selector.keys()) {
            ((Closeable) key.attachment()).close();
        }

        isRunning = false;
        selector.wakeup();
    }

    void addRemainingConnection(final InetSocketAddress address) {
        remainingConnections.add(address);
    }

    void addEstablishedConnection(final InetSocketAddress address) {
        establishedConnections.add(address);
    }

    void addIncomingConnection(final SocketChannel socketChannel) {
        try {
            socketChannel.configureBlocking(false);
            final var key = socketChannel.register(selector, SelectionKey.OP_READ);
            final var handler = new Handler(this, socketChannel, key);
            key.attach(handler);
            remainingIncomingConnections--;
        } catch (IOException e) {
            LOGGER.error("Failed to configure socket channel", e);
        }
    }

    public static void printBanner() {
        final InputStream inputStream = Benchmark.class.getClassLoader().getResourceAsStream("banner.txt");
        if (inputStream == null) {
            return;
        }

        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        final String banner = reader.lines().collect(Collectors.joining(System.lineSeparator()));

        System.out.print("\n");
        System.out.printf(banner, BuildConfig.VERSION, BuildConfig.BUILD_DATE, BuildConfig.GIT_BRANCH, BuildConfig.GIT_COMMIT);
        System.out.print("\n\n");
    }
}
