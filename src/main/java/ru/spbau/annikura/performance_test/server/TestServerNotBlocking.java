package ru.spbau.annikura.performance_test.server;

import ru.spbau.annikura.performance_test.server.tasks.SortingTask;
import ru.spbau.annikura.performance_test.server.tasks.TaskContext;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class TestServerNotBlocking implements PerformanceTestServerInterface {
    public void start(int port) {
        Logger.getAnonymousLogger().info("Starting not blocking server");
        ServerSocketChannel serverSocketChannel;
        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(port));
        } catch (IOException e) {
            Logger.getAnonymousLogger().severe("Unable to run server socket. Shutting down...: " + e.getMessage());
            return;
        }

        ReadingLoopRunnable readingLoop = new ReadingLoopRunnable();
        WritingLoopRunnable writingLoop = new WritingLoopRunnable();
        ExecutorService sortThreadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        new Thread(readingLoop).start();
        new Thread(writingLoop).start();

        while (true) {
            try {
                SocketChannel channel = serverSocketChannel.accept();

                TaskContext context = new TaskContext();
                TaskContext.ReadingTaskContext readContext = context.new ReadingTaskContext(channel);
                context.new WritingTaskContext(channel);
                channel.configureBlocking(false);

                readingLoop.addChannel(channel, readContext, sortingTaskContext -> {
                    sortThreadPool.submit(() -> {
                        new SortingTask().call(sortingTaskContext, writingTaskContext -> {
                            writingLoop.addChannel(channel, writingTaskContext, taskContext -> {
                                if (taskContext.isLast()) {
                                    readingLoop.remove(channel);
                                    writingLoop.remove(channel);
                                }
                            }, (writingTaskContext1, e) -> {
                                Logger.getAnonymousLogger().severe("Writing failed: " + e.getMessage());
                            });
                        }, (sortingTaskContext1, e) -> {
                            Logger.getAnonymousLogger().severe("Sort failed: " + e.getMessage());
                        });
                    });
                }, (readingTaskContext, e) -> {
                    Logger.getAnonymousLogger().severe("Reading failed: " + e.getMessage());
                });

                Logger.getAnonymousLogger().info("Accepted new connection");

            } catch (IOException e) {
                Logger.getAnonymousLogger().warning("Failed to establish connection with client: " + e.getMessage());
            }
        }
    }
}

