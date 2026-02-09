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

import java.util.*;
import java.util.stream.Collectors;

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
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 16)); // フォントサイズ調整
        statusLabel.setTextFill(Color.web("#e74c3c")); // 強調色

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
        // 全政党の与党フラグを一旦リセット
        parties.forEach(p -> p.setGovernment(false));

        // --- 1. 政党一覧タブ更新 (支持率順) ---
        partyListContainer.getChildren().clear();
        parties.stream()
                .sorted(Comparator.comparingInt(Party::getPopularity).reversed())
                .forEach(p -> partyListContainer.getChildren().add(createPartyCard(p)));

        // --- 2. 分析タブ更新 ---
        // まず議席順にソート
        parties.sort(Comparator.comparingInt(Party::getSeats).reversed());
        int totalSeats = parties.stream().mapToInt(Party::getSeats).sum();

        if (totalSeats == 0) {
            statusLabel.setText("まだ選挙が行われていません");
            pieChart.setData(FXCollections.observableArrayList());
            statsTable.setItems(FXCollections.observableArrayList());
            return;
        }

        // ★ 連立政権 形成ロジック
        List<Party> coalition = formCoalition(parties, totalSeats);

        // 連立に入った党にフラグを立てる
        coalition.forEach(p -> p.setGovernment(true));

        // 連立名を作成 (例: "自由・民進連立政権")
        String coalitionName = coalition.stream()
                .map(Party::getName)
                .collect(Collectors.joining("・")) + "連立政権";

        int coalitionSeats = coalition.stream().mapToInt(Party::getSeats).sum();
        double coalitionShareVal = (double)coalitionSeats / totalSeats * 100.0;
        String coalitionShare = String.format("%.1f%%", coalitionShareVal);

        // ステータス表示
        statusLabel.setText("【政権】" + coalitionName + " (" + coalitionSeats + "議席 / " + coalitionShare + ")");

        // ★ グラフ更新 (連立与党をまとめて先頭に配置)
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();


        // 1. 連立与党を追加
        for (Party p : coalition) {
            if (p.getSeats() > 0) {
                // 修正前: pieData.add(new PieChart.Data(p.getName() + "\n(与党)", p.getSeats()));
                // 修正後: 名前、(与党)、議席数 を表示
                pieData.add(new PieChart.Data(p.getName() + "\n(与党) " + p.getSeats() + "議席", p.getSeats()));
            }
        }
        // 2. 野党を追加
        for (Party p : parties) {
            if (!coalition.contains(p) && p.getSeats() > 0) {
                // 修正前: pieData.add(new PieChart.Data(p.getName(), p.getSeats()));
                // 修正後: 名前、議席数 を表示
                pieData.add(new PieChart.Data(p.getName() + " " + p.getSeats() + "議席", p.getSeats()));
            }
        }
        pieChart.setData(pieData);

        // 色付け (名前の最初の部分だけを使って色を取得するロジックはそのまま維持)
        for (PieChart.Data d : pieChart.getData()) {
            // 改行や空白で区切って、最初の要素（政党名）を取得
            String rawName = d.getName().split("[\n ]")[0];
            String hexColor = PartyColors.getHex(rawName);
            d.getNode().setStyle("-fx-pie-color: " + hexColor + ";");
        }

        // ★ リスト更新 (連立政権を行に追加)
        ObservableList<PartyStats> tableData = FXCollections.observableArrayList();

        // 先頭行に「連立政権」を追加 (rank=0)
        tableData.add(new PartyStats(0, "★ " + coalitionName, coalitionSeats, coalitionShare));

        int rank = 1;
        for (Party p : parties) {
            double share = (double) p.getSeats() / totalSeats * 100.0;
            // 与党入りしている場合は矢印をつける
            String nameDecor = coalition.contains(p) ? "  ↳ " + p.getName() : p.getName();
            tableData.add(new PartyStats(rank++, nameDecor, p.getSeats(), String.format("%.1f%%", share)));
        }
        statsTable.setItems(tableData);
    }

    // ★ 連立形成ロジック
    // ★ 連立形成ロジック (無所属除外対応版)
    private List<Party> formCoalition(List<Party> sortedParties, int totalSeats) {
        List<Party> coalition = new ArrayList<>();
        if (sortedParties.isEmpty()) return coalition;

        // 第1党は必ず入る
        Party leader = sortedParties.get(0);
        coalition.add(leader);

        // ★追加: 第一党が「無所属」なら連立を組まない（単独扱い）
        if (leader.getName().equals("無所属")) {
            return coalition;
        }

        int currentSeats = leader.getSeats();
        int majority = totalSeats / 2 + 1;

        // 単独過半数なら終了
        if (currentSeats >= majority) {
            return coalition;
        }

        // 連立パートナー候補 (第1党以外)
        List<Party> partners = new ArrayList<>(sortedParties);
        partners.remove(leader);

        // 協力度が高い順にソート
        partners.sort((p1, p2) -> Double.compare(leader.calculateCooperation(p2), leader.calculateCooperation(p1)));

        // 協力度の高い1つか2つの党と組む
        for (Party p : partners) {
            // ★追加: パートナーが「無所属」なら連立に加えない
            if (p.getName().equals("無所属")) continue;

            // 協力度が極端に低い(30未満)なら組まない
            if (leader.calculateCooperation(p) < 30.0) continue;

            coalition.add(p);
            currentSeats += p.getSeats();

            // 過半数到達したら終了
            if (currentSeats >= majority) break;

            // すでに3党(リーダー+2党)なら終了
            if (coalition.size() >= 3) break;
        }

        return coalition;
    }

    private HBox createPartyCard(Party p) {
        HBox card = new HBox(15);
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("party-card");

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
        ideologyBadge.getStyleClass().add("ideology-badge");

        Label popLabel = new Label("支持率: " + p.getPopularity() + "%");
        popLabel.getStyleClass().add("party-popularity");

        nameBox.getChildren().addAll(nameLabel, ideologyBadge, popLabel);

        Label description = new Label(p.getDescription());
        description.setFont(Font.font("System", 14));
        description.setWrapText(true);
        description.setMaxWidth(550);
        description.getStyleClass().add("party-description");

        VBox ideologyBox = createIdeologyBox(p.getIdeologies());
        ideologyBox.setVisible(false);
        ideologyBox.setManaged(false);
        ideologyBox.getStyleClass().add("ideology-box");

        card.setOnMouseClicked(event -> toggleIdeologyBox(ideologyBox));

        textContainer.getChildren().addAll(nameBox, description, ideologyBox);
        card.getChildren().addAll(colorIcon, textContainer);

        return card;
    }

    private void toggleIdeologyBox(VBox box) {
        boolean next = !box.isVisible();
        box.setVisible(next);
        box.setManaged(next);
    }

    private VBox createIdeologyBox(Map<String, Integer> ideologies) {
        VBox container = new VBox(6);
        container.setPadding(new Insets(8, 0, 0, 0));
        container.getStyleClass().add("ideology-container");

        Label header = new Label("イデオロギー指標 (0〜20)");
        header.getStyleClass().add("ideology-header");

        if (ideologies == null || ideologies.isEmpty()) {
            Label emptyLabel = new Label("詳細データはありません。");
            emptyLabel.getStyleClass().add("ideology-empty");
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
            name.getStyleClass().add("ideology-name");

            Integer value = entry.getValue();
            String scoreText = value == null ? "-" : String.valueOf(value);
            Label score = new Label(scoreText);
            score.getStyleClass().add("ideology-score");

            grid.addRow(row++, name, score);
        }

        container.getChildren().addAll(header, grid);
        return container;
    }

    private TableView<PartyStats> createTableView() {
        TableView<PartyStats> table = new TableView<>();

        TableColumn<PartyStats, Integer> colRank = new TableColumn<>("順位");
        colRank.setCellValueFactory(new PropertyValueFactory<>("rank"));
        colRank.setPrefWidth(50);

        // ★追加: 順位が0の場合は「政府」と表示する
        colRank.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item == 0 ? "政府" : item.toString());
                    if (item == 0) setStyle("-fx-font-weight: bold; -fx-text-fill: #e74c3c;");
                    else setStyle("");
                }
            }
        });

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