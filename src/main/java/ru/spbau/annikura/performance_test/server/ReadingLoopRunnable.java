package ru.spbau.annikura.performance_test.server;

import org.jetbrains.annotations.NotNull;
import ru.spbau.annikura.performance_test.server.tasks.IncompleteTask;
import ru.spbau.annikura.performance_test.server.tasks.ReadingRequestTask;
import ru.spbau.annikura.performance_test.server.tasks.TaskContext;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class ReadingLoopRunnable implements Runnable {
    volatile private Selector selector;
    private final HashMap<SocketChannel, SelectionKey> keys = new HashMap<>();
    private final TestServerNotBlocking server;


    public ReadingLoopRunnable(@NotNull TestServerNotBlocking serverLink) {
        server = serverLink;
    }

    {
        try {
            selector = Selector.open();
        } catch (IOException e) {
            Logger.getAnonymousLogger().severe("Failed to create selector: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        while (true) {
            int selected = 0;
            try {
                selected = selector.selectNow();
            } catch (IOException e) {
                Logger.getAnonymousLogger().severe("Reading selector failed: " + e.getMessage());
            }
            if (selected == 0) {
                continue;
            }
            Set<SelectionKey> keys = selector.selectedKeys();
            for (SelectionKey key : keys) {
                if (((IncompleteTask) key.attachment()).makeAttempt()) {
                    server.subscribeChannel((SocketChannel) key.channel());
                }
            }
            keys.clear();
        }
    }

    public void addChannel(@NotNull SocketChannel channel,
                                        @NotNull TaskContext.ReadingTaskContext readContext,
                                        @NotNull Consumer<TaskContext.SortingTaskContext> onSuccess,
                                        @NotNull BiConsumer<TaskContext.ReadingTaskContext, Exception> onFailure) {
        try {
            synchronized (keys) {
                keys.put(channel,
                        channel.register(selector, SelectionKey.OP_READ,
                                new ReadingRequestTask().createIncompleteTask(readContext, onSuccess, onFailure)));
            }
        } catch (ClosedChannelException e) {
            Logger.getAnonymousLogger().severe("Failed to register channel in reading loop");
            onFailure.accept(readContext, e);
        }
    }

    public void remove(@NotNull SocketChannel channel) {
        synchronized (keys) {
            keys.get(channel).cancel();
            keys.remove(channel);
        }
    }
}
