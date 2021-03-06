package de.hhu.bsinfo.nio.application;

import de.hhu.bsinfo.nio.benchmark.Benchmark;
import de.hhu.bsinfo.nio.application.util.InetSocketAddressConverter;
import de.hhu.bsinfo.nio.benchmark.BenchmarkConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;

@CommandLine.Command(
        name = "benchmark",
        description = "Benchmark application for Java NIO"
)
public class Application implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);
    private static final int DEFAULT_SERVER_PORT = 2998;

    @CommandLine.Option(
            names = {"-a", "--address"},
            description = "The address to bind to")
    private InetSocketAddress bindAddress = new InetSocketAddress(DEFAULT_SERVER_PORT);

    @CommandLine.Option(
            names = {"-i", "--incoming"},
            description = "The amount of incoming connection to wait for")
    private int incomingConnections = 0;

    @CommandLine.Option(
            names = {"-c", "--count"},
            description = "The amount of messages to send/receive per connection",
            required = true)
    private int operationCount;

    @CommandLine.Option(
            names = {"-s", "--size"},
            description = "The message size",
            required = true)
    private int operationSize;

    @CommandLine.Option(
            names = {"-t", "--threads"},
            description = "The thread pool size; Set it to zero, to not use a thread pool, but only the main thread instead (Default: 0)")
    private int threadCount = 0;

    @CommandLine.Option(
            names = {"-b", "--benchmark"},
            description = "The benchmark type (throughput/latency)")
    private BenchmarkConfiguration.BenchmarkType type = BenchmarkConfiguration.BenchmarkType.THROUGHPUT;

    @CommandLine.Parameters(
            description = "The remote addresses to connect to"
    )
    private Set<InetSocketAddress> outgoingConnections = new HashSet<>();

    private Benchmark benchmark;

    @Override
    public void run() {
        Benchmark.printBanner();
        final var configuration = new BenchmarkConfiguration(type, threadCount, operationCount, operationSize);

        try {
            benchmark = Benchmark.createBenchmark(configuration, bindAddress, outgoingConnections, incomingConnections);
        } catch (IOException e) {
            LOGGER.error("Failed to create benchmark instance", e);
            return;
        }

        final var shutdownHook = new Thread(() -> {
            LOGGER.info("Received shutdown signal");
            try {
                benchmark.close();
            } catch (IOException e) {
                LOGGER.error("Failed to close benchmark", e);
            }
        });

        Runtime.getRuntime().addShutdownHook(shutdownHook);
        benchmark.run();
        Runtime.getRuntime().removeShutdownHook(shutdownHook);
    }

    public static void main(final String... args) {
        final int exitCode = new CommandLine(new Application())
                .registerConverter(InetSocketAddress.class, new InetSocketAddressConverter(DEFAULT_SERVER_PORT))
                .setCaseInsensitiveEnumValuesAllowed(true)
                .execute(args);

        System.exit(exitCode);
    }
}
