package de.hhu.bsinfo.nio.application;

import de.hhu.bsinfo.nio.benchmark.Benchmark;
import de.hhu.bsinfo.nio.application.util.InetSocketAddressConverter;
import picocli.CommandLine;

import java.net.InetSocketAddress;

@CommandLine.Command(
        name = "benchmark",
        description = "Benchmark application for Java NIO"
)
public class Application implements Runnable {

    private static final int DEFAULT_SERVER_PORT = 2998;

    @Override
    public void run() {
        Benchmark.printBanner();
    }

    public static void main(String... args) {
        final int exitCode = new CommandLine(new Application())
                .registerConverter(InetSocketAddress.class, new InetSocketAddressConverter(DEFAULT_SERVER_PORT))
                .setCaseInsensitiveEnumValuesAllowed(true)
                .execute(args);

        System.exit(exitCode);
    }
}
