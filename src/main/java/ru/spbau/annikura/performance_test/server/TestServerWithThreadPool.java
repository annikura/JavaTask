package ru.spbau.annikura.performance_test.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class TestServerWithThreadPool implements PerformanceTestServerInterface {
    public void start(int port) {
        Logger.getAnonymousLogger().info("Starting thread pool server");
        ServerSocketChannel serverSocketChannel;
        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(port));
        } catch (IOException e) {
            Logger.getAnonymousLogger().severe("Unable to run server socket. Shutting down...: " + e.getMessage());
            return;
        }
        ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        while (true) {
            try {
                Runnable runnable = new TestServerWithThreadPoolRunnable(
                        serverSocketChannel.accept(),
                        threadPool, Executors.newSingleThreadExecutor());
                new Thread(runnable).start();
                Logger.getAnonymousLogger().info("Accepted new connection");
            } catch (IOException e) {
                Logger.getAnonymousLogger().warning("Failed to establish connection with client: " + e.getMessage());
            }
        }
    }
}
