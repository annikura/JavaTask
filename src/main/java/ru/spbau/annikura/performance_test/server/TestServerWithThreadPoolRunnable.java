package ru.spbau.annikura.performance_test.server;

import org.jetbrains.annotations.NotNull;
import ru.spbau.annikura.performance_test.server.tasks.ReadingRequestTask;
import ru.spbau.annikura.performance_test.server.tasks.SortingTask;
import ru.spbau.annikura.performance_test.server.tasks.TaskContext;
import ru.spbau.annikura.performance_test.server.tasks.WritingResponseTask;
import sun.rmi.runtime.Log;

import javax.xml.ws.Holder;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class TestServerWithThreadPoolRunnable implements Runnable {
    private SocketChannel channel;
    private final ExecutorService threadPool;
    private final ExecutorService outputService;

    public TestServerWithThreadPoolRunnable(@NotNull final SocketChannel channel,
                                            @NotNull final ExecutorService pool,
                                            @NotNull final ExecutorService outputService) {
        this.channel = channel;
        this.threadPool = pool;
        this.outputService = outputService;
    }

    @Override
    public void run() {
        Holder<Boolean> isLast = new Holder<>(false);
        while (!isLast.value) {
            TaskContext context = new TaskContext();
            context.new WritingTaskContext(channel);

            new ReadingRequestTask().call(context.new ReadingTaskContext(channel), sortingTaskContext -> {
                Logger.getAnonymousLogger().info("Read successfully");
                threadPool.submit(() -> {
                    new SortingTask().call(sortingTaskContext, writingTaskContext -> {
                        Logger.getAnonymousLogger().info("Sorted successfully.");
                        outputService.submit(() -> {
                            new WritingResponseTask().call(writingTaskContext, taskContext -> {
                                Logger.getAnonymousLogger().info("Wrote successfully");
                                isLast.value = taskContext.isLast();
                                Logger.getAnonymousLogger().info("Set isLast to " + isLast.value);
                            }, (writingTaskContext1, e) -> {
                                Logger.getAnonymousLogger().severe("Writing failed: " + e.getMessage());
                            });
                        });
                        Logger.getAnonymousLogger().info("Writing task was submitted");
                    }, (sortingTaskContext1, e) -> {
                        Logger.getAnonymousLogger().severe("Sort failed: " + e.getMessage());
                    });
                });
            }, (readingTaskContext, e) -> {
                Logger.getAnonymousLogger().severe("Reading failed: " + e.getMessage());
            });
        }
    }
}
