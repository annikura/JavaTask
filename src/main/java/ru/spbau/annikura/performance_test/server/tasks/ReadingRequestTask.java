package ru.spbau.annikura.performance_test.server.tasks;

import org.jetbrains.annotations.NotNull;
import ru.spbau.annikura.performance_test.PerformanceTestProtocol;

import java.nio.ByteBuffer;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ReadingRequestTask implements CallbackTask<TaskContext.ReadingTaskContext, TaskContext.SortingTaskContext> {
    public IncompleteTask createIncompleteTask(@NotNull final TaskContext.ReadingTaskContext context,
                                               @NotNull final Consumer<TaskContext.SortingTaskContext> onSuccess,
                                               @NotNull final BiConsumer<TaskContext.ReadingTaskContext, Exception> onFailure) {
        return new IncompleteTask() {
            private int requestSize = -1;
            private ByteBuffer buf = ByteBuffer.allocate(4);

            @Override
            public boolean makeAttempt() {
                try {
                    context.getChannel().read(buf);

                    if (buf.position() < buf.limit())
                        return false;
                    buf.flip();
                    if (requestSize == -1) {
                        requestSize = buf.getInt();
                        buf = ByteBuffer.allocate(requestSize);
                        return makeAttempt();
                    }
                    context.setStartRequestHandleTime();
                    PerformanceTestProtocol.SortRequest request = PerformanceTestProtocol.SortRequest.parseFrom(buf.array());
                    context.setIsLast(request.getIsLast());
                    onSuccess.accept(context.attachContext(context.getMainContext().new SortingTaskContext(request)));
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
        task.makeAttempt();
    }
}
