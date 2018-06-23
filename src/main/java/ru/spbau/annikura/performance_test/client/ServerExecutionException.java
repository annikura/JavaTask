package ru.spbau.annikura.performance_test.client;

import org.jetbrains.annotations.NotNull;

public class ServerExecutionException extends Exception {
    public ServerExecutionException(@NotNull String errorMessage) {
        super(errorMessage);
    }
}
