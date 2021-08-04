package de.hhu.bsinfo.nio.benchmark;

import de.hhu.bsinfo.nio.benchmark.result.Combiner;
import de.hhu.bsinfo.nio.benchmark.result.LatencyCombiner;
import de.hhu.bsinfo.nio.benchmark.result.ThroughputCombiner;
import de.hhu.bsinfo.nio.generated.BuildConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Set;
import java.util.stream.Collectors;

public class Benchmark implements Runnable, Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Benchmark.class);

    private final ConnectionReactor connectionReactor;
    private final BenchmarkReactor benchmarkReactor;

    private Benchmark(final Selector selector, final Combiner combiner, final BenchmarkConfiguration configuration, final SynchronizationCounter synchronizationCounter, final Set<InetSocketAddress> outgoingConnections, final int incomingConnections) {
        connectionReactor = new ConnectionReactor(selector, combiner, configuration, synchronizationCounter, outgoingConnections, incomingConnections);
        benchmarkReactor = new BenchmarkReactor(selector, combiner, synchronizationCounter);
    }

    public static Benchmark createBenchmark(final BenchmarkConfiguration configuration, final InetSocketAddress localAddress, final Set<InetSocketAddress> outgoingConnections, final int incomingConnections) throws IOException {
        LOGGER.info("Creating benchmark instance (localAddress: [{}], outgoingConnections: [{}])", localAddress, outgoingConnections);

        final var selector = Selector.open();
        final var combiner = configuration.getType() == BenchmarkConfiguration.BenchmarkType.THROUGHPUT ? new ThroughputCombiner() : new LatencyCombiner();
        final var synchronizationCounter = new SynchronizationCounter(outgoingConnections.size() + incomingConnections);
        final var benchmark = new Benchmark(selector, combiner, configuration, synchronizationCounter, outgoingConnections, incomingConnections);

        if (incomingConnections > 0) {
            final var serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(localAddress);

            final var serverKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            final var acceptor = new Acceptor(benchmark.connectionReactor, combiner, configuration, synchronizationCounter, serverSocketChannel, serverKey);
            serverKey.attach(acceptor);
        }

        return benchmark;
    }

    @Override
    public void run() {
        LOGGER.info("Starting connection reactor");
        connectionReactor.run();
        LOGGER.info("Finished connection reactor");

        LOGGER.info("Starting benchmark reactor");
        benchmarkReactor.run();
        LOGGER.info("Finished benchmark reactor");
    }

    @Override
    public void close() throws IOException {
        connectionReactor.close();
    }

    public static void printBanner() {
        final var inputStream = Benchmark.class.getClassLoader().getResourceAsStream("banner.txt");
        if (inputStream == null) {
            return;
        }

        final var reader = new BufferedReader(new InputStreamReader(inputStream));
        final var banner = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        final var provider = System.getProperty("java.nio.channels.spi.SelectorProvider");

        System.out.print("\n");
        System.out.printf(banner, BuildConfig.VERSION, BuildConfig.BUILD_DATE, BuildConfig.GIT_BRANCH, BuildConfig.GIT_COMMIT, provider == null ? "Default" : provider);
        System.out.print("\n\n");
    }
}
