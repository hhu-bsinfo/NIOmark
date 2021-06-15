package de.hhu.bsinfo.nio.benchmark;

import de.hhu.bsinfo.nio.generated.BuildConfig;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class Benchmark {

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
