package ru.spbau.annikura.performance_test.client;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.*;

public class PerformanceTester {
    private final int arraySize;
    private final int numOfClients;
    private final long requestDelay;
    private final int requestsPerClient;

    public PerformanceTester(int arraySize, int numOfClients, long requestDelay, int requestsPerClient) {
        this.arraySize = arraySize;
        this.numOfClients = numOfClients;
        this.requestDelay = requestDelay;
        this.requestsPerClient = requestsPerClient;
    }

    public TestResult startTest(@NotNull final String host, int port) throws IOException {
        int cores = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(cores);
        @NotNull ArrayList<Future<TestResult>> futureResults = new ArrayList<>();
        for (int i = 0; i < numOfClients; i++) {
            Callable<TestResult> client = new PerformanceTestClient(host, port, arraySize, requestDelay, requestsPerClient);
            futureResults.add(executor.submit(client));
        }
        TestResult result = new TestResult();

        for (Future<TestResult> futureResult : futureResults) {
            while(true) {
                try {
                    result.addTestResult(futureResult.get());
                    break;
                } catch (InterruptedException e) {
                    continue;
                } catch (ExecutionException e) {
                    return null;
                }
            }
        }

        return result;
    }
}