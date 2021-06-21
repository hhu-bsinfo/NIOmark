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

    private final ConnectionReactor connectionReactor;

    private Benchmark(final Selector selector, final Set<InetSocketAddress> outgoingConnections, final int incomingConnections) {
        connectionReactor = new ConnectionReactor(selector, outgoingConnections, incomingConnections);
    }

    public static Benchmark createBenchmark(final InetSocketAddress localAddress, final Set<InetSocketAddress> outgoingConnections, final int incomingConnections) throws IOException {
        LOGGER.info("Creating benchmark instance (localAddress: [{}], outgoingConnections: {})", localAddress, outgoingConnections);

        final var selector = Selector.open();
        final var benchmark = new Benchmark(selector, outgoingConnections, incomingConnections);

        if (incomingConnections > 0) {
            final var serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(localAddress);

            final var serverKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            final var acceptor = new Acceptor(benchmark.connectionReactor, serverSocketChannel, serverKey);
            serverKey.attach(acceptor);
        }

        return benchmark;
    }

    @Override
    public void run() {
        LOGGER.info("Starting connection reactor");
        connectionReactor.run();
        LOGGER.info("Finished connection reactor");

        LOGGER.info("Finished benchmark");
    }

    @Override
    public void close() throws IOException {
        connectionReactor.close();
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
