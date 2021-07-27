package de.hhu.bsinfo.nio.benchmark.result;

public class ThroughputCombiner extends Combiner {

    @Override
    protected Measurement combineMeasurements(Measurement[] measurements) {
        if (measurements.length == 0) {
            return new ThroughputMeasurement(0, 0);
        }

        double operationThroughput = 0;
        double dataThroughput = 0;
        for (final var measurement : measurements) {
            if (!(measurement instanceof ThroughputMeasurement)) {
                throw new IllegalArgumentException("Throughput combiner can only combine throughput measurements!");
            }

            operationThroughput += ((ThroughputMeasurement) measurement).getOperationThroughput();
            dataThroughput += ((ThroughputMeasurement) measurement).getDataThroughput();
        }

        final var combined = new ThroughputMeasurement(measurements[0].getOperationCount(), measurements[0].getOperationSize());
        combined.setOperationThroughput(operationThroughput);
        combined.setDataThroughput(dataThroughput);

        return combined;
    }
}
