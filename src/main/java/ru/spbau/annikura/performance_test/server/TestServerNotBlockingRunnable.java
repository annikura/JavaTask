package ru.spbau.annikura.performance_test.server;

import org.jetbrains.annotations.NotNull;

import java.nio.channels.SocketChannel;

public class TestServerNotBlockingRunnable implements Runnable {
    public TestServerNotBlockingRunnable(@NotNull final SocketChannel channel) {
    }
}
