package ru.spbau.annikura.performance_test.server;

import org.jetbrains.annotations.NotNull;
import ru.spbau.annikura.performance_test.server.tasks.ReadingRequestTask;
import ru.spbau.annikura.performance_test.server.tasks.SortingTask;
import ru.spbau.annikura.performance_test.server.tasks.TaskContext;
import ru.spbau.annikura.performance_test.server.tasks.WritingResponseTask;

import javax.xml.ws.Holder;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
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
            WritingOnSuccessHandler writingOnSuccessHandler = new WritingOnSuccessHandler(isLast);

            new ReadingRequestTask().call(context.new ReadingTaskContext(channel), sortingTaskContext -> {
                Logger.getAnonymousLogger().info("Read successfully");
                threadPool.submit(() -> {
                    new SortingTask().call(sortingTaskContext, writingTaskContext -> {
                        Logger.getAnonymousLogger().info("Sorted successfully.");
                        outputService.submit(() -> {
                            new WritingResponseTask().call(writingTaskContext, taskContext -> {
                                Logger.getAnonymousLogger().info("Wrote successfully");
                                isLast.value = taskContext.isLast();
                            }, (writingTaskContext1, e) -> {
                                Logger.getAnonymousLogger().severe("Writing failed: " + e.getMessage());
                                context.setErrorMessage(e.getMessage());
                                closeChannel(isLast, channel);
                            });
                        });
                    }, (sortingTaskContext1, e) -> {
                        Logger.getAnonymousLogger().severe("Sort failed: " + e.getMessage());
                        context.setErrorMessage(e.getMessage());
                        new WritingResponseTask().call(context.getAttachedContext(TaskContext.WritingTaskContext.class),
                                writingOnSuccessHandler, (writingTaskContext, e1) -> closeChannel(isLast, channel));
                    });
                });
            }, (readingTaskContext, e) -> {
                Logger.getAnonymousLogger().severe("Reading failed: " + e.getMessage());
                context.setErrorMessage(e.getMessage());
                new WritingResponseTask().call(context.getAttachedContext(TaskContext.WritingTaskContext.class),
                        writingOnSuccessHandler, (writingTaskContext, e1) -> closeChannel(isLast, channel));

            });
        }
    }

    private class WritingOnSuccessHandler implements Consumer<TaskContext> {
        private Holder<Boolean> holder;

        WritingOnSuccessHandler(Holder<Boolean> holder) {
            this.holder = holder;
        }

        @Override
        public void accept(TaskContext taskContext) {
            if (taskContext.isLast() || taskContext.getErrorMessage() != null) {
                closeChannel(holder, channel);
            }
        }
    }

    private void closeChannel(Holder<Boolean> holder, SocketChannel channel) {
        holder.value = true;
        try {
            channel.close();
        } catch (IOException ignored) { }
    }
}
