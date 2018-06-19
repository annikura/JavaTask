package ru.spbau.annikura.performance_test.server.tasks;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

@FunctionalInterface
public interface CallbackTask<C, T> {
    void call(C context, Consumer<T> onSuccess, BiConsumer<C, Exception> onFailure);
}
