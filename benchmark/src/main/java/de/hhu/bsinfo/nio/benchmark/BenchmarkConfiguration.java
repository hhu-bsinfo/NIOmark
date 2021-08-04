package de.hhu.bsinfo.nio.benchmark;

public class BenchmarkConfiguration {

    public enum BenchmarkType {
        THROUGHPUT,
        LATENCY
    }

    private final BenchmarkType type;
    private final int operationCount;
    private final int operationSize;

    public BenchmarkConfiguration(final BenchmarkType type, final int operationCount, final int operationSize) {
        this.type = type;
        this.operationCount = operationCount;
        this.operationSize = operationSize;
    }

    BenchmarkType getType() {
        return type;
    }

    int getOperationCount() {
        return operationCount;
    }

    int getOperationSize() {
        return operationSize;
    }
}
