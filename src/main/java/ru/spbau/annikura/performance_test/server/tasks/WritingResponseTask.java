package ru.spbau.annikura.performance_test.server.tasks;

import org.jetbrains.annotations.NotNull;
import ru.spbau.annikura.performance_test.PerformanceTestProtocol;

import java.nio.ByteBuffer;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Logger;

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
                    context.getMainContext().setFinishRequestHandleTime();
                    TaskContext.SortingTaskContext sortingTaskContext =
                            context.getMainContext().getAttachedContext(TaskContext.SortingTaskContext.class);
                    PerformanceTestProtocol.SortResponse response = PerformanceTestProtocol.SortResponse.newBuilder()
                            .addAllArrayElements(sortingTaskContext.getArray())
                            .setArraySize(sortingTaskContext.getArray().size())
                            .setStats(PerformanceTestProtocol.SortResponse.Statistics.newBuilder()
                                    .setSortTime(context.getMainContext().getSortTime())
                                    .setRequestTime(context.getMainContext().getRequestHandleTime())
                            ).build();
                    byte[] responseData = response.toByteArray();
                    Logger.getAnonymousLogger().info("Ready to write " + responseData.length);
                    buffer = ByteBuffer.allocate(4 + responseData.length);
                    buffer.clear();
                    buffer.putInt(responseData.length);
                    buffer.put(responseData);
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
