package ru.spbau.annikura.performance_test.client;

import org.jetbrains.annotations.NotNull;
import ru.spbau.annikura.performance_test.PerformanceTestProtocol;
import sun.rmi.runtime.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
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
    private final SocketChannel channel;

    public PerformanceTestClient(@NotNull String host, int port,
                                 int arraySize, long requestDelay, int requestsPerClient) throws IOException {
        this.arraySize = arraySize;
        delay = requestDelay;
        numOfRequests = requestsPerClient;
        channel = SocketChannel.open();
        channel.connect(new InetSocketAddress(host, port));
    }

    @NotNull
    public TestResult call() throws IOException {
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
            ByteBuffer buf = ByteBuffer.allocate(4 + requestData.length);
            buf.clear();
            buf.putInt(requestData.length);
            buf.put(requestData);
            buf.flip();
            Logger.getAnonymousLogger().info("Ready to write");
            channel.write(buf);
            Logger.getAnonymousLogger().info("Sent request");
            buf = ByteBuffer.allocate(4);
            buf.clear();

            Logger.getAnonymousLogger().info("Ready to read");
            channel.read(buf);
            Logger.getAnonymousLogger().info("Read " + buf.position() + " bytes");
            buf.flip();
            int responseDataSize = buf.getInt();
            Logger.getAnonymousLogger().info("Received response size: " + responseDataSize + " in thread " + Thread.currentThread().getName());
            buf = ByteBuffer.allocate(responseDataSize);
            buf.clear();
            channel.read(buf);
            Logger.getAnonymousLogger().info("Received response");

            @NotNull PerformanceTestProtocol.SortResponse response =
                    PerformanceTestProtocol.SortResponse.parseFrom(buf.array());
            assert validateSortResult(array, response.getArrayElementsList());
            PerformanceTestProtocol.SortResponse.Statistics stats = response.getStats();
            testResult.addRequestStats(stats.getSortTime(), stats.getRequestTime());
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
        channel.close();
    }
}
