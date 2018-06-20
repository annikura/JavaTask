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
        Logger.getAnonymousLogger().info("in write method");
        IncompleteTask task = createIncompleteTask(context, onSuccess, onFailure);
        while (!task.makeAttempt());
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
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
                    Logger.getAnonymousLogger().info("Writing buffer: " + bytesToHex(buffer.array()));
                }
                try {
                    context.getChannel().write(buffer);
                    Logger.getAnonymousLogger().info("Wrote " + buffer.position() + " bytes out of " + buffer.limit());
                    if (buffer.position() == buffer.limit()) {
                        onSuccess.accept(context.getMainContext());
                        return true;
                    }

                } catch (Exception e) {
                    onFailure.accept(context, e);
                }
                return false;
            }
        };
    }
}
