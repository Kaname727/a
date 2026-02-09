package jp.el;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList; // ★これを追加
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class PrefectureDetailView extends VBox {

    private final Label nameLabel;
    private final Label incomeLabel;
    private final Label elderlyLabel;
    private final PieChart industryChart;
    private final Runnable onClose;

    public PrefectureDetailView(Runnable onClose) {
        this.onClose = onClose;

        // --- スタイル設定 ---
        this.setAlignment(Pos.TOP_CENTER);
        this.setPadding(new Insets(20));
        this.setSpacing(15);
        this.setMaxSize(400, 450); // パネルのサイズ
        // 白背景、角丸、影付きのデザイン
        this.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 10, 0, 0, 5);");

        // --- タイトル部分 ---
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        nameLabel = new Label("都道府県名");
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 24));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("×");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-font-size: 20px; -fx-text-fill: gray; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> close());

        header.getChildren().addAll(nameLabel, spacer, closeBtn);

        // --- 統計データ表示 ---
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(20);
        statsGrid.setVgap(10);
        statsGrid.setAlignment(Pos.CENTER);

        statsGrid.add(createLabel("平均年収:", true), 0, 0);
        incomeLabel = createLabel("- 万円", false);
        statsGrid.add(incomeLabel, 1, 0);

        statsGrid.add(createLabel("高齢化率:", true), 0, 1);
        elderlyLabel = createLabel("- %", false);
        statsGrid.add(elderlyLabel, 1, 1);

        // --- 産業構造円グラフ ---
        industryChart = new PieChart();
        industryChart.setTitle("産業別就業人口");
        industryChart.setLabelsVisible(true); // ラベル表示
        industryChart.setLegendVisible(false);
        industryChart.setPrefHeight(200);

        this.getChildren().addAll(header, statsGrid, industryChart);
    }

    public void setData(PrefectureData data) {
        if (data == null) return;

        nameLabel.setText(data.getName());
        incomeLabel.setText(String.format("%,d 万円", data.getIncome()));
        elderlyLabel.setText(String.format("%.1f %%", data.getElderly()));

        // ★修正: 型を厳密に指定してエラーを防ぐ
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                new PieChart.Data(String.format("第1次 %.1f%%", data.getPrimary()), data.getPrimary()),
                new PieChart.Data(String.format("第2次 %.1f%%", data.getSecondary()), data.getSecondary()),
                new PieChart.Data(String.format("第3次 %.1f%%", data.getTertiary()), data.getTertiary())
        );

        industryChart.setData(pieData);

        // 色分け
        applyChartColor(0, "#2ecc71"); // 緑
        applyChartColor(1, "#3498db"); // 青
        applyChartColor(2, "#f1c40f"); // 黄
    }

    private void applyChartColor(int index, String hexColor) {
        // データセット直後はノードが生成されていないことがあるため、Platform.runLaterを使うと安全ですが
        // 簡易実装としてnullチェックを行います
        if (industryChart.getData().size() > index) {
            var node = industryChart.getData().get(index).getNode();
            if (node != null) {
                node.setStyle("-fx-pie-color: " + hexColor + ";");
            }
        }
    }

    private void close() {
        this.setVisible(false);
        if (onClose != null) onClose.run();
    }

    private Label createLabel(String text, boolean isBold) {
        Label l = new Label(text);
        l.setFont(Font.font("System", isBold ? FontWeight.BOLD : FontWeight.NORMAL, 16));
        return l;
    }
}