package ru.spbau.annikura.performance_test.server;

import org.jetbrains.annotations.NotNull;
import ru.spbau.annikura.performance_test.server.tasks.ReadingRequestTask;
import ru.spbau.annikura.performance_test.server.tasks.SortingTask;
import ru.spbau.annikura.performance_test.server.tasks.TaskContext;
import ru.spbau.annikura.performance_test.server.tasks.WritingResponseTask;

import javax.xml.ws.Holder;
import java.nio.channels.SocketChannel;
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

            TaskContext.ReadingTaskContext rContext = context.getAttachedContext(TaskContext.ReadingTaskContext.class);
            new ReadingRequestTask().call(rContext, sortingTaskContext -> {
                Logger.getAnonymousLogger().info("Read successfully.");
                new SortingTask().call(sortingTaskContext, writingTaskContext -> {
                    Logger.getAnonymousLogger().info("Sorted successfully.");
                    new WritingResponseTask().call(writingTaskContext, taskContext -> {
                        Logger.getAnonymousLogger().info("Wrote successfully.");
                        holder.value = !taskContext.isLast();
                        Logger.getAnonymousLogger().info("Updated holder value: " + holder.value);
                    }, (writingTaskContext1, e) -> {
                        Logger.getAnonymousLogger().severe("Writing failed: " + e.getMessage());
                    });
                }, (sortingTaskContext1, e) -> {
                    Logger.getAnonymousLogger().severe("Sort failed: " + e.getMessage());
                });
            }, (readingTaskContext, e) -> {
                Logger.getAnonymousLogger().severe("Reading failed: " + e.getMessage());
            });
        }
    }
}
