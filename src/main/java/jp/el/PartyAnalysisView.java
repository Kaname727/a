package jp.el;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PartyAnalysisView extends TabPane {

    private final PieChart pieChart;
    private final TableView<PartyStats> statsTable;
    private final Label statusLabel;
    private final VBox partyListContainer;

    public PartyAnalysisView() {
        this.setStyle("-fx-background-color: transparent;");
        this.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);

        // タブ1: 分析
        VBox analysisTabContent = new VBox(20);
        analysisTabContent.setPadding(new Insets(20));
        analysisTabContent.setStyle("-fx-background-color: white; -fx-background-radius: 10;");

        Label title = new Label("政党別 議席獲得状況");
        title.setFont(Font.font("System", FontWeight.BOLD, 20));

        HBox chartBox = new HBox(20);
        chartBox.setAlignment(Pos.CENTER);
        VBox.setVgrow(chartBox, Priority.ALWAYS);

        pieChart = new PieChart();
        pieChart.setTitle("議席占有率");
        pieChart.setLabelsVisible(true);
        pieChart.setLegendVisible(false);
        HBox.setHgrow(pieChart, Priority.ALWAYS);

        statsTable = createTableView();
        HBox.setHgrow(statsTable, Priority.ALWAYS);

        chartBox.getChildren().addAll(pieChart, statsTable);
        statusLabel = new Label("待機中...");
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        analysisTabContent.getChildren().addAll(title, statusLabel, chartBox);
        Tab tab1 = new Tab("📊 勢力分析", analysisTabContent);

        // タブ2: 一覧
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        partyListContainer = new VBox(15);
        partyListContainer.setPadding(new Insets(20));
        partyListContainer.setStyle("-fx-background-color: white;");

        scrollPane.setContent(partyListContainer);
        Tab tab2 = new Tab("📖 政党一覧", scrollPane);

        this.getTabs().addAll(tab1, tab2);
    }

    public void updateData(List<Party> parties) {
        // --- 1. 政党一覧タブ更新 ---
        partyListContainer.getChildren().clear();
        parties.stream()
                .sorted(Comparator.comparingInt(Party::getPopularity).reversed())
                .forEach(p -> partyListContainer.getChildren().add(createPartyCard(p)));

        // --- 2. 分析タブ更新 ---
        parties.sort(Comparator.comparingInt(Party::getSeats).reversed());
        int totalSeats = parties.stream().mapToInt(Party::getSeats).sum();

        if (totalSeats == 0) {
            statusLabel.setText("まだ選挙が行われていません");
            return;
        }

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        for (Party p : parties) {
            if (p.getSeats() > 0) {
                pieData.add(new PieChart.Data(p.getName() + " (" + p.getSeats() + ")", p.getSeats()));
            }
        }
        pieChart.setData(pieData);

        for (PieChart.Data d : pieChart.getData()) {
            String partyName = d.getName().split(" \\(")[0];
            String hexColor = PartyColors.getHex(partyName);
            d.getNode().setStyle("-fx-pie-color: " + hexColor + ";");
        }

        ObservableList<PartyStats> tableData = FXCollections.observableArrayList();
        int rank = 1;
        for (Party p : parties) {
            double share = (double) p.getSeats() / totalSeats * 100.0;
            tableData.add(new PartyStats(rank++, p.getName(), p.getSeats(), String.format("%.1f%%", share)));
        }
        statsTable.setItems(tableData);
        statusLabel.setText("総議席数: " + totalSeats + "  /  過半数ライン: " + (totalSeats / 2 + 1));
    }

    private HBox createPartyCard(Party p) {
        HBox card = new HBox(15);
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: #f9f9f9; -fx-background-radius: 8; -fx-border-color: #e0e0e0; -fx-border-radius: 8;");

        Color color = PartyColors.get(p.getName());
        Circle colorIcon = new Circle(25, color);
        colorIcon.setStroke(Color.GRAY);
        colorIcon.setStrokeWidth(1);

        VBox textContainer = new VBox(5);

        HBox nameBox = new HBox(10);
        nameBox.setAlignment(Pos.BASELINE_LEFT);
        Label nameLabel = new Label(p.getName());
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 18));

        Label ideologyBadge = new Label(p.getIdeology());
        ideologyBadge.setStyle("-fx-background-color: #bdc3c7; -fx-text-fill: white; -fx-padding: 3 8; -fx-background-radius: 10; -fx-font-size: 10px;");

        Label popLabel = new Label("支持率: " + p.getPopularity() + "%");
        popLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");

        nameBox.getChildren().addAll(nameLabel, ideologyBadge, popLabel);

        Text description = new Text(p.getDescription());
        description.setFont(Font.font("System", 14));
        description.setWrappingWidth(550);

        VBox ideologyBox = createIdeologyBox(p.getIdeologies());
        ideologyBox.setVisible(false);
        ideologyBox.setManaged(false);

        card.setOnMouseClicked(event -> toggleIdeologyBox(ideologyBox));

        textContainer.getChildren().addAll(nameBox, description, ideologyBox);
        card.getChildren().addAll(colorIcon, textContainer);

        return card;
    }

    private VBox createIdeologyBox(Map<String, Integer> ideologies) {
        VBox container = new VBox(6);
        container.setPadding(new Insets(8, 0, 0, 0));

        Label header = new Label("イデオロギー指標 (0〜20)");
        header.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        if (ideologies == null || ideologies.isEmpty()) {
            Label emptyLabel = new Label("詳細データはありません。");
            emptyLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 12px;");
            container.getChildren().addAll(header, emptyLabel);
            return container;
        }

        Map<String, Integer> ordered = new LinkedHashMap<>();
        ordered.put("保守", ideologies.get("保守"));
        ordered.put("リベラル", ideologies.get("リベラル"));
        ordered.put("ポピュリズム", ideologies.get("ポピュリズム"));
        ordered.put("リバタリアニズム", ideologies.get("リバタリアニズム"));
        ordered.put("環境主義", ideologies.get("環境主義"));
        ordered.put("積極財政", ideologies.get("積極財政"));
        ordered.put("緊縮財政", ideologies.get("緊縮財政"));
        ordered.put("ナショナリズム", ideologies.get("ナショナリズム"));

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(6);

        int row = 0;
        for (Map.Entry<String, Integer> entry : ordered.entrySet()) {
            Label name = new Label(entry.getKey());
            name.setStyle("-fx-font-size: 12px; -fx-text-fill: #34495e;");

            Integer value = entry.getValue();
            String scoreText = value == null ? "-" : String.valueOf(value);
            Label score = new Label(scoreText);
            score.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

            grid.addRow(row++, name, score);
        }

        container.getChildren().addAll(header, grid);
        return container;
    }

    private void toggleIdeologyBox(VBox box) {
        boolean next = !box.isVisible();
        box.setVisible(next);
        box.setManaged(next);
    }

    private TableView<PartyStats> createTableView() {
        TableView<PartyStats> table = new TableView<>();

        TableColumn<PartyStats, Integer> colRank = new TableColumn<>("順位");
        colRank.setCellValueFactory(new PropertyValueFactory<>("rank"));
        colRank.setPrefWidth(50);

        TableColumn<PartyStats, String> colName = new TableColumn<>("政党名");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setPrefWidth(120);

        TableColumn<PartyStats, Integer> colSeats = new TableColumn<>("議席数");
        colSeats.setCellValueFactory(new PropertyValueFactory<>("seats"));
        colSeats.setPrefWidth(80);
        colSeats.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<PartyStats, String> colShare = new TableColumn<>("占有率");
        colShare.setCellValueFactory(new PropertyValueFactory<>("share"));
        colShare.setPrefWidth(80);
        colShare.setStyle("-fx-alignment: CENTER-RIGHT;");

        table.getColumns().addAll(colRank, colName, colSeats, colShare);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        return table;
    }

    public static class PartyStats {
        private final int rank; private final String name; private final int seats; private final String share;
        public PartyStats(int rank, String name, int seats, String share) {
            this.rank = rank; this.name = name; this.seats = seats; this.share = share;
        }
        public int getRank() { return rank; }
        public String getName() { return name; }
        public int getSeats() { return seats; }
        public String getShare() { return share; }
    }
}
