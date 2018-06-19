package ru.spbau.annikura.performance_test.server;

import org.jetbrains.annotations.NotNull;

public class PerformanceTestServerStarter {
    private PerformanceTestServerInterface server;
    PerformanceTestServerStarter(@NotNull ServerMode mode) {
        switch (mode) {
            case BLOCKING_SPLITTED:
                server = new TestServerSimple();
                break;
            case BLOCKING_THREAD_POOLLED:
                server = new TestServerWithThreadPool();
                break;
            case NOT_BLOCKING:
                server = new TestServerNotBlocking();
                break;
        }
    }

    public void start(int port) {
        server.start(port);
    }

    public enum ServerMode {
        BLOCKING_SPLITTED,
        BLOCKING_THREAD_POOLLED,
        NOT_BLOCKING
    }

}
