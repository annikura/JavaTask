package ru.spbau.annikura.performance_test.client;

import org.jetbrains.annotations.NotNull;
import ru.spbau.annikura.performance_test.PerformanceTestProtocol;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import static java.util.Collections.sort;

public class PerformanceTestClient implements Callable<TestResult> {
    private final int arraySize;
    private final long delay;
    private final int numOfRequests;
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;


    public PerformanceTestClient(@NotNull String host, int port,
                                 int arraySize, long requestDelay, int requestsPerClient) throws IOException {
        this.arraySize = arraySize;
        delay = requestDelay;
        numOfRequests = requestsPerClient;
        socket = new Socket(host, port);
        in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    }

    @NotNull
    public TestResult call() throws IOException, ServerExecutionException {
        long startClientTime = System.currentTimeMillis();

        TestResult testResult = new TestResult();
        @NotNull Random random = new Random();
        @NotNull ArrayList<Integer> array = new ArrayList<>(arraySize);
        for (int i = 0; i < numOfRequests; i++) {
            Logger.getAnonymousLogger().info("Starting request #" + i);
            for (int j = 0; j < arraySize; j++) {
                array.add(i, random.nextInt());
            }
            @NotNull PerformanceTestProtocol.SortRequest request = PerformanceTestProtocol.SortRequest.newBuilder()
                    .setArraySize(arraySize)
                    .setIsLast(i == (numOfRequests - 1))
                    .addAllArrayElements(array).build();
            byte[] requestData = request.toByteArray();
            out.writeInt(requestData.length);
            out.write(requestData);
            out.flush();
            Logger.getAnonymousLogger().info("Sent request");

            int responseDataSize = in.readInt();
            byte[] responseData = new byte[responseDataSize];
            int readBytes = 0;
            while (readBytes < responseDataSize) {
                readBytes += in.read(responseData, readBytes, responseDataSize - readBytes);
            }
            Logger.getAnonymousLogger().info("Received response");

            @NotNull PerformanceTestProtocol.SortResponse response =
                    PerformanceTestProtocol.SortResponse.parseFrom(responseData);
            assert validateSortResult(array, response.getArrayElementsList());
            PerformanceTestProtocol.SortResponse.Statistics stats = response.getStats();
            testResult.addRequestStats(stats.getSortTime(), stats.getRequestTime());
            if (response.hasErrorMessage()) {
                throw new ServerExecutionException(response.getErrorMessage());
            }
            Logger.getAnonymousLogger().info("Counted stats");
            if (i != numOfRequests - 1) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ignore) {
                }
            }
        }
        testResult.addClientStats(System.currentTimeMillis() - startClientTime);
        terminate();
        return testResult;
    }

    private static boolean validateSortResult(@NotNull List<Integer> array, @NotNull List<Integer> arrayElementsList) {
        sort(array);
        for (int i = 0; i < array.size(); i++) {
            if (!array.get(i).equals(arrayElementsList.get(i)))
                return false;
        }
        return true;
    }

    private void terminate() throws IOException {
        socket.close();
    }
}
