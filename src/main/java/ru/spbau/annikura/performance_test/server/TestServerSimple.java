package ru.spbau.annikura.performance_test.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.logging.Logger;

public class TestServerSimple implements PerformanceTestServerInterface {
    public void start(int port) {
        Logger.getAnonymousLogger().info("Starting simple server");
        ServerSocketChannel serverSocketChannel;
        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(port));
        } catch (IOException e) {
            Logger.getAnonymousLogger().severe("Unable to run server socket. Shutting down...: " + e.getMessage());
            return;
        }

        while (true) {
            try {
                Runnable runnable = new TestServerSimpleRunnable(serverSocketChannel.accept());
                new Thread(runnable).start();
                Logger.getAnonymousLogger().info("Accepted new connection");
            } catch (IOException e) {
                Logger.getAnonymousLogger().warning("Failed to establish connection with client: " + e.getMessage());
            }
        }
    }
}
