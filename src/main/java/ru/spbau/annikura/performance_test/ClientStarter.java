package ru.spbau.annikura.performance_test;

import javafx.application.Application;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;
import ru.spbau.annikura.performance_test.ui.ScenesCollection;

/**
 * Main application class. Starts application.
 */
public class ClientStarter extends Application {
    private ScenesCollection scenesCollection = new ScenesCollection();

    /**
     * Starts application stage
     * @param primaryStage stage to palace scenes on
     */
    @Override
    public void start(@NotNull Stage primaryStage) {
        primaryStage.setTitle("Performance tester");

        primaryStage.setScene(scenesCollection.newLogInScene(900, 500, primaryStage));
        primaryStage.show();
    }

    /**
     * Runs application
     * @param args input parameters of the application. Not expected here.
     */
    public static void main(@NotNull String[] args) {
        launch(args);
    }
}