package ru.spbau.annikura.performance_test.server.tasks;

import org.jetbrains.annotations.NotNull;
import ru.spbau.annikura.performance_test.PerformanceTestProtocol;

import java.nio.ByteBuffer;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class ReadingRequestTask implements CallbackTask<TaskContext.ReadingTaskContext, TaskContext.SortingTaskContext> {
    public IncompleteTask createIncompleteTask(@NotNull final TaskContext.ReadingTaskContext context,
                                               @NotNull final Consumer<TaskContext.SortingTaskContext> onSuccess,
                                               @NotNull final BiConsumer<TaskContext.ReadingTaskContext, Exception> onFailure) {
        return new IncompleteTask() {
            private int requestSize = -1;
            private ByteBuffer buf;

            @Override
            public boolean makeAttempt() {
                try {
                    if (buf == null) {
                        buf = ByteBuffer.allocate(4);
                        buf.clear();
                    }
                    int read = context.getChannel().read(buf);
                    if (context.getChannel().isBlocking() && read == -1)
                        return true;
                    if (buf.position() < buf.limit())
                        return false;
                    buf.flip();
                    if (requestSize == -1) {
                        requestSize = buf.getInt();
                        Logger.getAnonymousLogger().info("Read request size: " + requestSize);
                        buf = ByteBuffer.allocate(requestSize);
                        buf.clear();
                        return makeAttempt();
                    }
                    context.getMainContext().setStartRequestHandleTime();
                    PerformanceTestProtocol.SortRequest request = PerformanceTestProtocol.SortRequest.parseFrom(buf.array());
                    context.getMainContext().setIsLast(request.getIsLast());

                    onSuccess.accept(context.getMainContext().new SortingTaskContext(request));
                    return true;
                } catch (Exception e) {
                    onFailure.accept(context, e);
                    return false;
                }
            }
        };
    }

    @Override
    public void call(@NotNull TaskContext.ReadingTaskContext context,
                     @NotNull Consumer<TaskContext.SortingTaskContext> onSuccess,
                     @NotNull BiConsumer<TaskContext.ReadingTaskContext, Exception> onFailure) {
        IncompleteTask task = createIncompleteTask(context, onSuccess, onFailure);
        while (!task.makeAttempt());
    }
}
