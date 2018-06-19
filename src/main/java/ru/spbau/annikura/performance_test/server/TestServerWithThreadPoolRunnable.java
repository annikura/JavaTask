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
            new ReadingRequestTask().call(context.new ReadingTaskContext(channel), sortingTaskContext -> {
                threadPool.submit(() -> {
                    new SortingTask().call(sortingTaskContext, writingTaskContext -> {
                        outputService.submit(() -> {
                            new WritingResponseTask().call(writingTaskContext, taskContext -> {
                                isLast.value = taskContext.isLast();
                            }, (writingTaskContext1, e) -> {
                                Logger.getAnonymousLogger().severe("Writing failed: " + e.getMessage());
                            });
                        });
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
