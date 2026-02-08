package jp.el;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class TitleView {
    // 画面遷移のためのコールバックを受け取る
    private Runnable onStartAction;
    private Runnable onListAction;

    public TitleView(Runnable onStartAction, Runnable onListAction) {
        this.onStartAction = onStartAction;
        this.onListAction = onListAction;
    }

    public Parent getView() {
        VBox root = new VBox(30);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #2c3e50, #4ca1af);");

        Label titleLabel = new Label("衆議院選挙\nシミュレーター");
        titleLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 50));
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Button startButton = createButton("シミュレーション開始", "#e74c3c");
        startButton.setOnAction(e -> onStartAction.run());

        Button listButton = createButton("候補者・政党一覧", "#3498db");
        listButton.setOnAction(e -> onListAction.run());

        root.getChildren().addAll(titleLabel, startButton, listButton);
        return root;
    }

    private Button createButton(String text, String color) {
        Button btn = new Button(text);
        btn.setPrefSize(300, 60);
        btn.setStyle(
                "-fx-background-color: " + color + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 20px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-background-radius: 30; " +
                        "-fx-cursor: hand;"
        );
        return btn;
    }
}