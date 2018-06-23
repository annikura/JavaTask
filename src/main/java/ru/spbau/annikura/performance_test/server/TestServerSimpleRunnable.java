package ru.spbau.annikura.performance_test.server;

import org.jetbrains.annotations.NotNull;
import ru.spbau.annikura.performance_test.server.tasks.ReadingRequestTask;
import ru.spbau.annikura.performance_test.server.tasks.SortingTask;
import ru.spbau.annikura.performance_test.server.tasks.TaskContext;
import ru.spbau.annikura.performance_test.server.tasks.WritingResponseTask;

import javax.xml.ws.Holder;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class TestServerSimpleRunnable implements Runnable {
    private final SocketChannel channel;

    public TestServerSimpleRunnable(@NotNull final SocketChannel channel) {
        this.channel = channel;
    }

    public void run() {
        Logger.getAnonymousLogger().info("Started new splitted Runnable");
        final Holder<Boolean> holder = new Holder<>(true);
        while (holder.value) {
            TaskContext context = new TaskContext();
            context.new WritingTaskContext(channel);
            context.new ReadingTaskContext(channel);

            WritingOnSuccessHandler writingOnSuccessHandler = new WritingOnSuccessHandler(holder);

            TaskContext.ReadingTaskContext rContext = context.getAttachedContext(TaskContext.ReadingTaskContext.class);
            new ReadingRequestTask().call(rContext, sortingTaskContext -> {
                Logger.getAnonymousLogger().info("Read successfully.");
                new SortingTask().call(sortingTaskContext, writingTaskContext -> {
                    Logger.getAnonymousLogger().info("Sorted successfully.");
                    new WritingResponseTask().call(writingTaskContext, writingOnSuccessHandler, (writingTaskContext1, e) -> {
                        Logger.getAnonymousLogger().severe("Writing failed: " + e.getMessage());
                        writingTaskContext.getMainContext().setErrorMessage(e.getMessage());
                        closeChannel(holder, channel);
                    });
                }, (sortingTaskContext1, e) -> {
                    Logger.getAnonymousLogger().severe("Sort failed: " + e.getMessage());
                    context.setErrorMessage(e.getMessage());
                    new WritingResponseTask().call(
                            context.getAttachedContext(TaskContext.WritingTaskContext.class),
                            writingOnSuccessHandler,
                            (writingTaskContext, e1) -> closeChannel(holder, channel));
                });
            }, (readingTaskContext, e) -> {
                Logger.getAnonymousLogger().severe("Reading failed: " + e.getMessage());
                context.setErrorMessage(e.getMessage());
                new WritingResponseTask().call(
                        context.getAttachedContext(TaskContext.WritingTaskContext.class),
                        writingOnSuccessHandler,
                        (writingTaskContext, e1) -> closeChannel(holder, channel));
            });
        }
    }

    private void closeChannel(Holder<Boolean> holder, SocketChannel channel) {
        holder.value = false;
        try {
            channel.close();
        } catch (IOException ignored) {
            // nothing can be done
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
}
