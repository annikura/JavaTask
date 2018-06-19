package ru.spbau.annikura.performance_test.server.tasks;

import org.jetbrains.annotations.NotNull;
import ru.spbau.annikura.performance_test.PerformanceTestProtocol;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.List;

public class TaskContext {
    private HashMap<Class<?>, Object> attachedContexts = new HashMap<>();
    private boolean isLast = false;

    private long startSortTime;
    private long finishedSortTime;

    private long startRequestHandleTime;
    private long finishRequestHandleTime;

    public TaskContext getMainContext() {
        return this;
    }

    void setIsLast(boolean isLast) {
        this.isLast = isLast;
    }

    void setStartSortTime() {
        startSortTime = System.currentTimeMillis();
    }

    void setFinishedSortTime() {
        finishedSortTime = System.currentTimeMillis();
    }

    void setStartRequestHandleTime() {
        startRequestHandleTime = System.currentTimeMillis();
    }

    void setFinishRequestHandleTime() {
        finishRequestHandleTime = System.currentTimeMillis();
    }

    public long getSortTime() {
        if (finishedSortTime == 0) return 0;
        return finishedSortTime - startSortTime;
    }

    public long getRequestHandleTime() {
        if (finishRequestHandleTime == 0) return 0;
        return finishRequestHandleTime - startRequestHandleTime;
    }

    public <T> T attachContext(T context) {
        attachedContexts.put(context.getClass(), context);
        return context;
    }

    public <T> T getAttachedContext(Class<T> clazz) {
        return (T) attachedContexts.get(clazz);
    }

    public boolean isLast() {
        return isLast;
    }

    public class ReadingTaskContext extends TaskContext {
        private SocketChannel channel;

        public ReadingTaskContext(@NotNull final SocketChannel channel) {
            this.channel = channel;
            attachedContexts.put(ReadingTaskContext.class, this);
        }

        public SocketChannel getChannel() {
            return channel;
        }

    }

    public class WritingTaskContext extends TaskContext {
        private final SocketChannel channel;
        public WritingTaskContext(@NotNull final SocketChannel channel) {
            this.channel = channel;
            attachedContexts.put(WritingTaskContext.class, this);
        }

        public SocketChannel getChannel() {
            return channel;
        }
    }

    public class SortingTaskContext extends TaskContext {
        private List<Integer> array;
        public SortingTaskContext(@NotNull final PerformanceTestProtocol.SortRequest request) {
            array = request.getArrayElementsList();
            isLast = request.getIsLast();
            attachedContexts.put(SortingTaskContext.class, this);
        }

        public List<Integer> getArray() {
            return array;
        }
    }
}
