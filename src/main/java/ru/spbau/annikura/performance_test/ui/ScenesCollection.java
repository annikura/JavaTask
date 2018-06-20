package ru.spbau.annikura.performance_test.ui;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;
import ru.spbau.annikura.performance_test.client.PerformanceTester;
import ru.spbau.annikura.performance_test.client.TestResult;

import java.io.IOException;
import java.util.logging.Logger;

public class ScenesCollection {
    private String host;
    private int port;

    private int from = 10;
    private int stepSize = 10;
    private int numOfSteps = 10;

    private TestResult[] simpleServerResults;
    private TestResult[] threadPoolServerResults;
    private TestResult[] notBlockingServerResults;

    private ChartType chartType;
    private ParameterType parameterType;

    private int fromArraySize;
    private int fromDelayTime;
    private int fromNumOfClients;

    private final static String MAIN_CSS = "-fx-background-color: #808080";
    private final static String BUTTONS_CSS =   "-fx-background-color: #4d4d4d;" +
            "-fx-text-fill: #FFFFFF";
    private final static String TEXT_CSS = "-fx-text-fill: #FFFFFF;";

    private enum ChartType {
        SORT_TIME_AVERAGE,
        CLIENT_TIME_AVERAGE,
        REQUEST_HANDLING_TIME_AVERAGE;

        public double retrieveValue(@NotNull TestResult result) {
            switch (this) {
                case SORT_TIME_AVERAGE:
                    return result.getAvgSortTime();
                case CLIENT_TIME_AVERAGE:
                    return result.getAvgClientTime();
                case REQUEST_HANDLING_TIME_AVERAGE:
                    return result.getAvgRequestTime();
            }
            return 0;
        }
    }

    private enum ParameterType {
        DELAY_TIME,
        NUM_OF_CLIENTS,
        ARRAY_SIZE;
    }

    public Scene newLogInScene(final double width, final double height, @NotNull final Stage stage) {
        VBox body = new VBox();

        VBox vBox = new VBox(10);
        final TextField portField = new TextField();
        portField.setPromptText("Port number");
        final TextField serverField = new TextField();
        serverField.setPromptText("Server url");
        Button okButton = new Button("Next");

        vBox.getChildren().addAll(portField, serverField, okButton);
        vBox.setAlignment(Pos.CENTER);
        vBox.setMaxWidth(300);

        body.setAlignment(Pos.CENTER);
        body.getChildren().addAll(vBox);

        okButton.setOnAction(event -> {
            String port = portField.getCharacters().toString();
            String url = serverField.getCharacters().toString();

            boolean portIsValid = port.length() <= 5 && port.matches("\\d+");
            if (!portIsValid) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Invalid port");
                alert.setHeaderText("Port is invalid. Please, try again");
                alert.setContentText("Port should be an integer from 0 to 65535.");
                alert.showAndWait();
                return;
            }

            host = url;
            this.port = Integer.valueOf(port);

            stage.setScene(newSettingsMenu(
                    stage.getScene().getWidth(),
                    stage.getScene().getHeight(), stage));
        });

        okButton.setStyle(BUTTONS_CSS);
        body.setStyle(MAIN_CSS);
        return new Scene(body, width, height);
    }

    private Scene newSettingsMenu(double width, double height, Stage stage) {
        HBox body = new HBox();
        body.setAlignment(Pos.CENTER);

        VBox innerBox = new VBox(20);
        innerBox.setAlignment(Pos.CENTER);

        Label title = new Label("Settings");
        title.setStyle("-fx-font: normal bold 20px 'serif'; -fx-text-fill: #FFFFFF");

        ToggleGroup paramenterGroup = new ToggleGroup();
        final RadioButton arraySizeParameter = new RadioButton("Array size");
        final RadioButton numOfClientsParameter = new RadioButton("Number of clients");
        final RadioButton timeDeltaParameter = new RadioButton("Delay between requests");
        arraySizeParameter.setStyle(TEXT_CSS);
        numOfClientsParameter.setStyle(TEXT_CSS);
        timeDeltaParameter.setStyle(TEXT_CSS);
        paramenterGroup.getToggles().addAll(arraySizeParameter, numOfClientsParameter, timeDeltaParameter);

        VBox toggles = new VBox(10);
        toggles.getChildren().addAll(arraySizeParameter, numOfClientsParameter, timeDeltaParameter);

        GridPane fieldsPane = new GridPane();
        fieldsPane.setMinSize(200, 0);
        fieldsPane.setHgap(5);
        fieldsPane.setVgap(5);
        fieldsPane.setAlignment(Pos.CENTER);

        Label fromLabel = new Label("From");
        Label stepLabel = new Label("Step size");
        Label numOfStepsLabel = new Label("#Steps");

        Label arrayElementsLabel = new Label("Array elements (quantity)");
        Label timeDeltaLabel = new Label("Time delta (ms)");
        Label numOfClientsLabel = new Label("Number of clients (quantity)");

        fromLabel.setStyle(TEXT_CSS);
        stepLabel.setStyle(TEXT_CSS);
        numOfStepsLabel.setStyle(TEXT_CSS);
        arrayElementsLabel.setStyle(TEXT_CSS);
        timeDeltaLabel.setStyle(TEXT_CSS);
        numOfClientsLabel.setStyle(TEXT_CSS);

        TextField fromArrayElementsField = new TextField();
        TextField fromTimeDeltaField = new TextField();
        TextField fromNumOfClientsField = new TextField();

        TextField stepArrayElementsField = new TextField();
        stepArrayElementsField.setDisable(true);
        TextField stepTimeDeltaField = new TextField();
        stepTimeDeltaField.setDisable(true);
        TextField stepNumOfClientsField = new TextField();
        stepNumOfClientsField.setDisable(true);

        TextField numOfStepsArrayElemetsField = new TextField();
        numOfStepsArrayElemetsField.setDisable(true);
        TextField numOfStepsTimeDeltaField = new TextField();
        numOfStepsTimeDeltaField.setDisable(true);
        TextField numOfStepsNumOfClientsField = new TextField();
        numOfStepsNumOfClientsField.setDisable(true);

        fieldsPane.add(fromLabel, 1, 0);
        fieldsPane.add(stepLabel, 2, 0);
        fieldsPane.add(numOfStepsLabel, 3, 0);

        fieldsPane.add(arrayElementsLabel, 0, 1);
        fieldsPane.add(timeDeltaLabel, 0, 2);
        fieldsPane.add(numOfClientsLabel, 0, 3);


        fieldsPane.add(fromArrayElementsField, 1, 1);
        fieldsPane.add(stepArrayElementsField, 2, 1);
        fieldsPane.add(numOfStepsArrayElemetsField, 3, 1);

        fieldsPane.add(fromTimeDeltaField, 1, 2);
        fieldsPane.add(stepTimeDeltaField, 2, 2);
        fieldsPane.add(numOfStepsTimeDeltaField, 3, 2);

        fieldsPane.add(fromNumOfClientsField, 1, 3);
        fieldsPane.add(stepNumOfClientsField, 2, 3);
        fieldsPane.add(numOfStepsNumOfClientsField, 3, 3);

        arraySizeParameter.selectedProperty().addListener(e -> {
            if (arraySizeParameter.isSelected()) {
                stepArrayElementsField.setDisable(false);
                numOfStepsArrayElemetsField.setDisable(false);
                stepNumOfClientsField.setDisable(true);
                stepNumOfClientsField.clear();
                numOfStepsNumOfClientsField.setDisable(true);
                numOfStepsNumOfClientsField.clear();
                stepTimeDeltaField.setDisable(true);
                stepTimeDeltaField.clear();
                numOfStepsTimeDeltaField.setDisable(true);
                numOfStepsTimeDeltaField.clear();
            }
        });
        numOfClientsParameter.selectedProperty().addListener(e -> {
            if (numOfClientsParameter.isSelected()) {
                stepArrayElementsField.setDisable(true);
                stepArrayElementsField.clear();
                numOfStepsArrayElemetsField.setDisable(true);
                numOfStepsArrayElemetsField.clear();
                stepNumOfClientsField.setDisable(false);
                numOfStepsNumOfClientsField.setDisable(false);
                stepTimeDeltaField.setDisable(true);
                stepTimeDeltaField.clear();
                numOfStepsTimeDeltaField.setDisable(true);
                numOfStepsTimeDeltaField.clear();
            }
        });
        timeDeltaParameter.selectedProperty().addListener(e -> {
            if (timeDeltaParameter.isSelected()) {
                stepArrayElementsField.setDisable(true);
                stepArrayElementsField.clear();
                numOfStepsArrayElemetsField.setDisable(true);
                numOfStepsArrayElemetsField.clear();
                stepNumOfClientsField.setDisable(true);
                stepNumOfClientsField.clear();
                numOfStepsNumOfClientsField.setDisable(true);
                numOfStepsNumOfClientsField.clear();

                stepTimeDeltaField.setDisable(false);
                numOfStepsTimeDeltaField.setDisable(false);
            }
        });

        ToggleGroup chartTypeToggle = new ToggleGroup();
        RadioButton sortTimeToggle = new RadioButton("Sort time avg");
        RadioButton requestHandleTimeToggle = new RadioButton("Request handle time avg");
        RadioButton clientTimeToggle = new RadioButton("Client time avg");
        chartTypeToggle.getToggles().addAll(sortTimeToggle, requestHandleTimeToggle, clientTimeToggle);
        HBox chartTypeBox = new HBox(10);
        chartTypeBox.getChildren().addAll(sortTimeToggle, requestHandleTimeToggle, clientTimeToggle);

        Button backButton = new Button("Back");
        backButton.setStyle(BUTTONS_CSS);
        Button nextButton = new Button("Next");
        nextButton.setStyle(BUTTONS_CSS);
        HBox buttonBox = new HBox(20);
        buttonBox.getChildren().addAll(backButton, nextButton);

        backButton.setOnAction(e -> {
            stage.setScene(newLogInScene(stage.getScene().getWidth(), stage.getScene().getHeight(), stage));
        });
        nextButton.setOnAction(e -> {
            if (arraySizeParameter.isSelected()) parameterType = ParameterType.ARRAY_SIZE;
            if (timeDeltaParameter.isSelected()) parameterType = ParameterType.DELAY_TIME;
            if (numOfClientsParameter.isSelected()) parameterType = ParameterType.NUM_OF_CLIENTS;

            if (sortTimeToggle.isSelected()) chartType = ChartType.SORT_TIME_AVERAGE;
            if (requestHandleTimeToggle.isSelected()) chartType = ChartType.REQUEST_HANDLING_TIME_AVERAGE;
            if (clientTimeToggle.isSelected()) chartType = ChartType.CLIENT_TIME_AVERAGE;

            if (parameterType.equals(ParameterType.ARRAY_SIZE)) {
                from = Integer.valueOf(fromArrayElementsField.getText());
                stepSize = Integer.valueOf(stepArrayElementsField.getText());
                numOfSteps = Integer.valueOf(numOfStepsArrayElemetsField.getText());
            }

            if (parameterType.equals(ParameterType.DELAY_TIME)) {
                from = Integer.valueOf(fromTimeDeltaField.getText());
                stepSize = Integer.valueOf(stepTimeDeltaField.getText());
                numOfSteps = Integer.valueOf(numOfStepsTimeDeltaField.getText());
            }

            if (parameterType.equals(ParameterType.NUM_OF_CLIENTS)) {
                from = Integer.valueOf(fromNumOfClientsField.getText());
                stepSize = Integer.valueOf(stepNumOfClientsField.getText());
                numOfSteps = Integer.valueOf(numOfStepsNumOfClientsField.getText());
            }

            fromArraySize = Integer.valueOf(fromArrayElementsField.getCharacters().toString());
            fromNumOfClients = Integer.valueOf(fromNumOfClientsField.getCharacters().toString());
            fromDelayTime = Integer.valueOf(fromTimeDeltaField.getCharacters().toString());
            stage.setScene(newWaitScene(stage.getScene().getWidth(), stage.getScene().getHeight(), stage));
            countData();
            stage.setScene(newChartScene(stage.getScene().getWidth(), stage.getScene().getHeight(), stage));
        });

        innerBox.getChildren().addAll(title, toggles, fieldsPane, chartTypeBox, buttonBox);
        body.getChildren().addAll(innerBox);
        body.setStyle(MAIN_CSS);
        return new Scene(body, width, height);
    }

    private void countData() {
        simpleServerResults = new TestResult[numOfSteps];
        threadPoolServerResults = new TestResult[numOfSteps];
        notBlockingServerResults = new TestResult[numOfSteps];

        for (int i = 0; i < numOfSteps; i++) {
            int arraySize = fromArraySize;
            int numOfClients = fromNumOfClients;
            int delay = fromDelayTime;

            switch (parameterType) {
                case ARRAY_SIZE:
                    arraySize = arraySize + i * stepSize;
                    break;
                case DELAY_TIME:
                    delay = delay + i * stepSize;
                    break;
                case NUM_OF_CLIENTS:
                    numOfClients = numOfClients + i * stepSize;
            }
            PerformanceTester tester = new PerformanceTester(arraySize, numOfClients, delay, 4);
            try {
                simpleServerResults[i] = tester.startTest(host, port);
                Logger.getAnonymousLogger().info("Simple done");
                threadPoolServerResults[i] = tester.startTest(host, port + 1);
                Logger.getAnonymousLogger().info("Pool done");
                notBlockingServerResults[i] = tester.startTest(host, port + 2);
            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Could not connect to server");
                alert.setHeaderText("Probably server is invalid. Please, try again");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            }
        }
    }

    private Scene newWaitScene(double width, double height, Stage stage) {
        VBox body = new VBox();
        Label wait = new Label("Please, wait");
        wait.setStyle(TEXT_CSS);
        body.setAlignment(Pos.CENTER);
        body.getChildren().addAll(wait);
        return new Scene(body, width, height);
    }

    public Scene newChartScene(double width, double height, @NotNull Stage stage) {
        VBox body = new VBox(10);
        body.setAlignment(Pos.CENTER);
        Button backButton = new Button("Back");
        backButton.setStyle(BUTTONS_CSS);
        backButton.setOnAction(e -> {
            stage.setScene(newSettingsMenu(stage.getScene().getWidth(), stage.getScene().getHeight(), stage));
        });

        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        XYChart.Series simpleServerSeries = new XYChart.Series();
        simpleServerSeries.setName("Simple server data");
        XYChart.Series threadPoolServerSeries = new XYChart.Series();
        threadPoolServerSeries.setName("Thread pool server data");
        XYChart.Series notBlockingServerSeries = new XYChart.Series();
        notBlockingServerSeries.setName("Not blocking server data");

        for (int i = 0; i < numOfSteps; i++) {
            simpleServerSeries.getData().add(new XYChart.Data<>(
                    from + stepSize * i, chartType.retrieveValue(simpleServerResults[i])));
            threadPoolServerSeries.getData().add(new XYChart.Data<>(
                    from + stepSize * i, chartType.retrieveValue(threadPoolServerResults[i])));
            notBlockingServerSeries.getData().add(new XYChart.Data<>(
                    from + stepSize * i, chartType.retrieveValue(notBlockingServerResults[i])));
        }

        lineChart.getData().addAll(simpleServerSeries, threadPoolServerSeries, notBlockingServerSeries);
        body.getChildren().addAll(lineChart, backButton);
        return new Scene(body, width, height);
    }
}