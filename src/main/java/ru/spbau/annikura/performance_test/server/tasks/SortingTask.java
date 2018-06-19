package ru.spbau.annikura.performance_test.server.tasks;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SortingTask implements CallbackTask<TaskContext.SortingTaskContext, TaskContext.WritingTaskContext> {
    @Override
    public void call(@NotNull TaskContext.SortingTaskContext context,
                     @NotNull Consumer<TaskContext.WritingTaskContext> onSuccess,
                     @NotNull BiConsumer<TaskContext.SortingTaskContext, Exception> onFailure) {
        context.setStartSortTime();
        sort(context.getArray());
        context.setFinishedSortTime();
        onSuccess.accept(context.getAttachedContext(TaskContext.WritingTaskContext.class));
    }

    private void sort(@NotNull List<Integer> array) {
        for (int i = 0; i < array.size(); i++) {
            for (int j = 1; j < array.size() - i; j++) {
                if (array.get(j) < array.get(j - 1)) {
                    int tmp = array.get(j);
                    array.set(j, array.get(j - 1));
                    array.set(j - 1, tmp);
                }
            }
        }
    }
}