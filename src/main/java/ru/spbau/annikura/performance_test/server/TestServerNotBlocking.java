package ru.spbau.annikura.performance_test.server;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.spbau.annikura.performance_test.server.tasks.SortingTask;
import ru.spbau.annikura.performance_test.server.tasks.TaskContext;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class TestServerNotBlocking implements PerformanceTestServerInterface {
    private final ReadingLoopRunnable readingLoop = new ReadingLoopRunnable(this);
    private final WritingLoopRunnable writingLoop = new WritingLoopRunnable();
    private final ExecutorService sortThreadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());


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


        new Thread(readingLoop).start();
        new Thread(writingLoop).start();

        while (true) {
            try {
                SocketChannel channel = serverSocketChannel.accept();
                channel.configureBlocking(false);
                subscribeChannel(channel);

            } catch (IOException e) {
                Logger.getAnonymousLogger().warning("Failed to establish connection with client: " + e.getMessage());
            }
        }
    }

    public void subscribeChannel(@NotNull SocketChannel channel) {
        TaskContext context = new TaskContext();
        TaskContext.ReadingTaskContext readContext = context.new ReadingTaskContext(channel);
        context.new WritingTaskContext(channel);

        WritingOnSuccessHandler writingOnSuccessHandler = new WritingOnSuccessHandler(channel);


        readingLoop.addChannel(channel, readContext, sortingTaskContext -> {
            sortThreadPool.submit(() -> {
                new SortingTask().call(sortingTaskContext, writingTaskContext -> {
                    writingLoop.addChannel(channel, writingTaskContext, writingOnSuccessHandler, (writingTaskContext1, e) -> {
                        Logger.getAnonymousLogger().severe("Writing failed: " + e.getMessage());
                        context.setErrorMessage(e.getMessage());
                        closeChannel(channel);

                    });
                }, (sortingTaskContext1, e) -> {
                    Logger.getAnonymousLogger().severe("Sort failed: " + e.getMessage());
                    context.setErrorMessage(e.getMessage());
                    writingLoop.addChannel(channel,
                            context.getAttachedContext(TaskContext.WritingTaskContext.class),
                            writingOnSuccessHandler, (writingTaskContext, e1) -> {
                                closeChannel(channel);
                            });
                });
            });
        }, (readingTaskContext, e) -> {
            Logger.getAnonymousLogger().severe("Reading failed: " + e.getMessage());
            context.setErrorMessage(e.getMessage());
            writingLoop.addChannel(channel, context.getAttachedContext(TaskContext.WritingTaskContext.class),
                    writingOnSuccessHandler, ((writingTaskContext, e1) -> {
                        closeChannel(channel);
                    }));
        });
        Logger.getAnonymousLogger().info("Accepted new connection");

    }
    private class WritingOnSuccessHandler implements Consumer<TaskContext> {
        private SocketChannel channel;

        WritingOnSuccessHandler(SocketChannel channel) {
            this.channel = channel;
        }

        @Override
        public void accept(TaskContext taskContext) {
            if (taskContext.isLast() || taskContext.getErrorMessage() != null) {
                closeChannel(channel);
            }
        }
    }

    private void closeChannel(SocketChannel channel) {
        readingLoop.remove(channel);
        writingLoop.remove(channel);
        try {
            channel.close();
        } catch (IOException ignored) {
            // nothing can be done.
        }
    }
}

