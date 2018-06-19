package ru.spbau.annikura.performance_test.server.tasks;

@FunctionalInterface
public interface IncompleteTask {
    boolean makeAttempt();
}
