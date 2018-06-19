package ru.spbau.annikura.performance_test.server.tasks;

import org.jetbrains.annotations.NotNull;
import ru.spbau.annikura.performance_test.PerformanceTestProtocol;

import java.nio.ByteBuffer;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class WritingResponseTask implements CallbackTask<TaskContext.WritingTaskContext, TaskContext> {
    @Override
    public void call(@NotNull final TaskContext.WritingTaskContext context,
                     @NotNull final Consumer<TaskContext> onSuccess,
                     @NotNull final BiConsumer<TaskContext.WritingTaskContext, Exception> onFailure) {
        IncompleteTask task = createIncompleteTask(context, onSuccess, onFailure);
        task.makeAttempt();
    }


    public IncompleteTask createIncompleteTask(@NotNull final TaskContext.WritingTaskContext context,
                                               @NotNull final Consumer<TaskContext> onSuccess,
                                               @NotNull final BiConsumer<TaskContext.WritingTaskContext, Exception> onFailure) {
        return new IncompleteTask() {
            private ByteBuffer buffer = null;

            @Override
            public boolean makeAttempt() {
                if (buffer == null) {
                    context.setFinishRequestHandleTime();
                    TaskContext.SortingTaskContext sortingTaskContext =
                            context.getAttachedContext(TaskContext.SortingTaskContext.class);
                    PerformanceTestProtocol.SortResponse response = PerformanceTestProtocol.SortResponse.newBuilder()
                            .addAllArrayElements(sortingTaskContext.getArray())
                            .setArraySize(sortingTaskContext.getArray().size())
                            .setStats(PerformanceTestProtocol.SortResponse.Statistics.newBuilder()
                                    .setSortTime(context.getSortTime())
                                    .setRequestTime(context.getRequestHandleTime())
                            ).build();
                    buffer = ByteBuffer.allocate(4 + response.getSerializedSize());
                    buffer.putInt(response.getSerializedSize());
                    buffer.put(response.toByteArray());
                    buffer.flip();
                }
                try {
                    context.getChannel().write(buffer);
                    if (buffer.position() == buffer.limit()) {
                        onSuccess.accept(context.getMainContext());
                    }

                } catch (Exception e) {
                    onFailure.accept(context, e);
                }
                return false;
            }
        };
    }
}
