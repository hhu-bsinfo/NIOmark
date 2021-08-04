package de.hhu.bsinfo.nio.benchmark.result;

import java.util.ArrayList;

public class LatencyCombiner extends Combiner {

    @Override
    protected Measurement combineMeasurements(Measurement[] measurements) {
        final var latencySet = new ArrayList<Long>();
        for (final var measurement : measurements) {
            if (!(measurement instanceof LatencyMeasurement)) {
                throw new IllegalArgumentException("Latency combiner can only combine latency measurements!");
            }

            for (final long latency : ((LatencyMeasurement) (measurement)).getStatistics().getTimesArray()) {
                latencySet.add(latency);
            }
        }

        return new LatencyMeasurement(measurements[0].getOperationCount(), measurements[0].getOperationSize(), latencySet.stream().mapToLong(Long::longValue).toArray());
    }
}
