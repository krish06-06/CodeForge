package com.codeforge.snapshot;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class AISnapshotView {

    private final BorderPane root;
    private final Label problemValue;
    private final Label causeValue;
    private final Label fixValue;
    private final Label confidenceValue;

    public AISnapshotView() {
        Label title = new Label("AI Snapshot");
        title.getStyleClass().add("section-label");

        problemValue = createValueLabel();
        causeValue = createValueLabel();
        fixValue = createValueLabel();
        confidenceValue = createValueLabel();
        confidenceValue.getStyleClass().add("snapshot-confidence-value");

        VBox content = new VBox(
            12,
            createField("Problem", problemValue),
            createField("Cause", causeValue),
            createField("Fix", fixValue),
            createField("Confidence", confidenceValue)
        );
        content.getStyleClass().add("snapshot-card");

        root = new BorderPane(content);
        root.setTop(title);
        root.setPadding(new Insets(10));
        root.getStyleClass().add("snapshot-shell");

        setResult(AISnapshotResult.idle());
    }

    public BorderPane getView() {
        return root;
    }

    public void setResult(AISnapshotResult result) {
        AISnapshotResult resolved = result == null ? AISnapshotResult.idle() : result;
        Platform.runLater(() -> {
            problemValue.setText(resolved.getProblem());
            causeValue.setText(resolved.getCause());
            fixValue.setText(resolved.getFix());
            confidenceValue.setText(resolved.getConfidence());
            updateConfidenceStyle(resolved.getConfidence());
        });
    }

    private VBox createField(String heading, Label valueLabel) {
        Label headingLabel = new Label(heading);
        headingLabel.getStyleClass().add("snapshot-heading");

        VBox field = new VBox(4, headingLabel, valueLabel);
        field.getStyleClass().add("snapshot-field");
        return field;
    }

    private Label createValueLabel() {
        Label label = new Label();
        label.setWrapText(true);
        label.getStyleClass().add("snapshot-value");
        return label;
    }

    private void updateConfidenceStyle(String confidence) {
        confidenceValue.getStyleClass().removeAll(
            "snapshot-confidence-low",
            "snapshot-confidence-medium",
            "snapshot-confidence-high"
        );

        String styleClass = switch (confidence == null ? "" : confidence.toLowerCase()) {
            case "low" -> "snapshot-confidence-low";
            case "high" -> "snapshot-confidence-high";
            default -> "snapshot-confidence-medium";
        };
        confidenceValue.getStyleClass().add(styleClass);
    }
}
