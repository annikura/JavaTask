package ru.spbau.annikura.performance_test.client;

import org.jetbrains.annotations.NotNull;

public class TestResult {
    private int requests = 0;
    private int clients = 0;

    private long sortTimeSum = 0;
    private long requestTimeSum = 0;
    private long clientTimeSum = 0;

    public void addTestResult(@NotNull final TestResult result) {
        requests += result.requests;
        clients += result.clients;

        sortTimeSum += result.sortTimeSum;
        requestTimeSum += result.requestTimeSum;
        clientTimeSum += result.clientTimeSum;
    }

    public void addRequestStats(long sortTime, long requestTime) {
        requests++;
        sortTimeSum += sortTime;
        requestTimeSum += requestTime;
    }

    public void addClientStats(long clientTime) {
        clients++;
        clientTimeSum += clientTime;
    }

    public double getAvgRequestTime() {
        if (requests == 0) return 0;
        return (double) requestTimeSum / (double) requests;
    }

    public double getAvgSortTime() {
        if (requests == 0) return 0;
        return (double) sortTimeSum / (double) requests;
    }

    public double getAvgClientTime() {
        if (clients == 0) return 0;
        return (double) clientTimeSum/ (double) clients;
    }
}
