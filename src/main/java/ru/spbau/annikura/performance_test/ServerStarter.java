package ru.spbau.annikura.performance_test;

import ru.spbau.annikura.performance_test.server.PerformanceTestServerStarter;

public class ServerStarter {
    public static void main(String[] args) {
        new Thread(() -> new PerformanceTestServerStarter(PerformanceTestServerStarter.ServerMode.BLOCKING_SPLITTED).start(7777)).start();
        new Thread(() -> new PerformanceTestServerStarter(PerformanceTestServerStarter.ServerMode.BLOCKING_THREAD_POOLLED).start(7778)).start();
        new Thread(() -> new PerformanceTestServerStarter(PerformanceTestServerStarter.ServerMode.NOT_BLOCKING).start(7779)).start();
    }
}
