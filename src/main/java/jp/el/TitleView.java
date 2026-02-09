package jp.el;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
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
        root.getStyleClass().add("title-root");

        Label titleLabel = new Label("衆議院選挙\nシミュレーター");
        titleLabel.getStyleClass().add("title-label");
        titleLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 50));
        titleLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Button startButton = createButton("シミュレーション開始");
        startButton.getStyleClass().add("title-button-primary");
        startButton.setOnAction(e -> onStartAction.run());

        Button listButton = createButton("候補者・政党一覧");
        listButton.getStyleClass().add("title-button-secondary");
        listButton.setOnAction(e -> onListAction.run());

        root.getChildren().addAll(titleLabel, startButton, listButton);
        return root;
    }

    private Button createButton(String text) {
        Button btn = new Button(text);
        btn.setPrefSize(300, 60);
        btn.getStyleClass().add("title-button");
        return btn;
    }
}
