package de.hhu.bsinfo.nio.benchmark.result;

import java.util.List;
import java.util.ArrayList;

public class ThroughputCombiner {

    private static final ThroughputCombiner INSTANCE = new ThroughputCombiner();

    private final List<ThroughputMeasurement> measurements = new ArrayList<>();

    public static ThroughputCombiner getInstance() {
        return INSTANCE;
    }

    public void addMeasurement(final ThroughputMeasurement measurement) {
        measurements.add(measurement);
    }

    public ThroughputMeasurement getCombinedMeasurement() {
        double operationThroughput = 0;
        double dataThroughput = 0;
        for (final var measurement : measurements) {
            operationThroughput += measurement.getOperationThroughput();
            dataThroughput += measurement.getDataThroughput();
        }

        final var combined = new ThroughputMeasurement(measurements.get(0).getOperationCount(), measurements.get(0).getOperationSize());
        combined.setOperationThroughput(operationThroughput);
        combined.setDataThroughput(dataThroughput);

        return combined;
    }
}
