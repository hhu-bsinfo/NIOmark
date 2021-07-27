package de.hhu.bsinfo.nio.benchmark;

public class BenchmarkConfiguration {

    private final int operationCount;
    private final int operationSize;

    public BenchmarkConfiguration(int operationCount, int operationSize) {
        this.operationCount = operationCount;
        this.operationSize = operationSize;
    }

    public int getOperationCount() {
        return operationCount;
    }

    public int getOperationSize() {
        return operationSize;
    }
}
