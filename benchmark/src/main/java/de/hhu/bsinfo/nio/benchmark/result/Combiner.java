package de.hhu.bsinfo.nio.benchmark.result;

import java.util.ArrayList;
import java.util.List;

public abstract class Combiner {

    private final List<Measurement> measurements = new ArrayList<>();

    public void addMeasurement(final Measurement measurement) {
        measurements.add(measurement);
    }

    public Measurement combine() {
        return combineMeasurements(measurements.toArray(new Measurement[0]));
    }

    public Measurement[] getMeasurements() {
        return measurements.toArray(new Measurement[0]);
    }

    protected abstract Measurement combineMeasurements(final Measurement[] measurements);

}
