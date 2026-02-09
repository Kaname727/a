package jp.el;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.*;

public class ResultView {
    private final ElectionData data;
    private final Runnable onBackAction;
    private final boolean isSimulationMode;

    private ListView<District> districtListView;
    private JapanMapView mapView;
    private StackPane mapWrapper; // ★追加: 地図とチェックボックスをまとめるラッパー
    private PartyAnalysisView analysisView;
    private Button actionButton;

    public ResultView(ElectionData data, boolean isSimulationMode, Runnable onBackAction) {
        this.data = data;
        this.isSimulationMode = isSimulationMode;
        this.onBackAction = onBackAction;
    }

    public Parent getView() {
        VBox root = new VBox(15);
        root.getStyleClass().add("root-pane");
        root.setAlignment(Pos.TOP_CENTER);

        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header-bar");

        Label title = new Label(isSimulationMode ? "衆院選シミュレーター" : "候補者データ一覧");
        title.getStyleClass().add("header-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ToggleButton listBtn = new ToggleButton("📋 リスト");
        ToggleButton mapBtn = new ToggleButton("🗺️ 地図");
        ToggleButton chartBtn = new ToggleButton("📊 分析");

        ToggleGroup group = new ToggleGroup();
        listBtn.setToggleGroup(group);
        mapBtn.setToggleGroup(group);
        chartBtn.setToggleGroup(group);

        listBtn.setSelected(true);
        listBtn.getStyleClass().add("toggle-button");
        mapBtn.getStyleClass().add("toggle-button");
        chartBtn.getStyleClass().add("toggle-button");

        actionButton = new Button("一斉投票を実行");
        actionButton.getStyleClass().add("action-button");
        if (!isSimulationMode) actionButton.setVisible(false);

        header.getChildren().addAll(title, spacer, listBtn, mapBtn, chartBtn, actionButton);

        StackPane contentStack = new StackPane();
        VBox.setVgrow(contentStack, Priority.ALWAYS);

        // 1. リスト
        districtListView = new ListView<>();
        districtListView.setItems(FXCollections.observableArrayList(data.getDistricts()));
        districtListView.setCellFactory(param -> new DistrictCell(isSimulationMode));
        districtListView.setStyle("-fx-background-color: transparent;");

        // 2. 地図 (★修正: チェックボックス付きのラッパーを作成)
        mapView = new JapanMapView();

        // 地図上の議席数表示切替チェックボックス
        CheckBox labelCheck = new CheckBox("議席数を表示");
        labelCheck.setSelected(true);
        labelCheck.getStyleClass().add("map-toggle");
        labelCheck.setOnAction(e -> mapView.setLabelsVisible(labelCheck.isSelected()));

        // 地図とチェックボックスを重ねる
        mapWrapper = new StackPane(mapView, labelCheck);
        StackPane.setAlignment(labelCheck, Pos.TOP_RIGHT); // 右上に配置
        StackPane.setMargin(labelCheck, new Insets(10));
        mapWrapper.setVisible(false);

        // 3. 分析
        analysisView = new PartyAnalysisView();
        analysisView.setVisible(false);
        analysisView.updateData(data.getParties());

        contentStack.getChildren().addAll(districtListView, mapWrapper, analysisView);

        // --- イベントハンドラ ---
        // マップ切り替え時は mapView ではなく mapWrapper を表示制御する
        listBtn.setOnAction(e -> switchView(districtListView, mapWrapper, analysisView));

        mapBtn.setOnAction(e -> {
            switchView(mapWrapper, districtListView, analysisView);
            if(isSimulationMode) updateMapAnimation();
        });

        chartBtn.setOnAction(e -> {
            switchView(analysisView, districtListView, mapWrapper);
            if(isSimulationMode) analysisView.updateData(data.getParties());
        });

        actionButton.setOnAction(e -> {
            runSimulation();
            districtListView.refresh();
            if (mapWrapper.isVisible()) updateMapAnimation();
            if (analysisView.isVisible()) analysisView.updateData(data.getParties());

            actionButton.setDisable(true);
            actionButton.setText("投票終了");
        });

        Button backBtn = new Button("タイトルへ戻る");
        backBtn.getStyleClass().add("back-button");
        backBtn.setOnAction(e -> onBackAction.run());

        root.getChildren().addAll(header, contentStack, backBtn);
        return root;
    }

    private void switchView(javafx.scene.Node show, javafx.scene.Node... hides) {
        show.setVisible(true);
        for(javafx.scene.Node h : hides) h.setVisible(false);
    }

    private void runSimulation() {
        Random rand = new Random();
        data.getParties().forEach(Party::reset);
        data.getDistricts().forEach(d -> d.getCandidates().forEach(Candidate::resetVotes));

        for (District d : data.getDistricts()) {
            int totalVoters = 3000 + rand.nextInt(3000);
            List<Candidate> candidates = d.getCandidates();

            int totalWeight = 0;
            int[] weights = new int[candidates.size()];

            for (int i = 0; i < candidates.size(); i++) {
                Party p = candidates.get(i).getParty();
                int weight = p.getPopularity() + rand.nextInt(15);
                if (weight <= 0) weight = 1;
                weights[i] = weight;
                totalWeight += weight;
            }

            for(int v = 0; v < totalVoters; v++) {
                int r = rand.nextInt(totalWeight);
                for (int i = 0; i < candidates.size(); i++) {
                    r -= weights[i];
                    if (r < 0) {
                        candidates.get(i).addVote();
                        break;
                    }
                }
            }

            Candidate w = d.getWinner();
            if(w != null) w.getParty().addSeat();
        }
    }

    private void updateMapAnimation() {
        Map<String, String> prefWinners = new HashMap<>();
        Map<String, Integer> prefSeats = new HashMap<>();

        Map<String, Map<String, Integer>> fullStats = new HashMap<>();
        for (District d : data.getDistricts()) {
            Candidate w = d.getWinner();
            if(w==null) continue;
            String pref = getPrefectureName(d.getName());
            fullStats.putIfAbsent(pref, new HashMap<>());
            Map<String, Integer> counts = fullStats.get(pref);
            counts.put(w.getParty().getName(), counts.getOrDefault(w.getParty().getName(), 0) + 1);
        }

        for(String pref : fullStats.keySet()) {
            Map.Entry<String, Integer> top = fullStats.get(pref).entrySet().stream()
                    .max(Map.Entry.comparingByValue()).orElse(null);
            if(top != null) {
                prefWinners.put(pref, top.getKey());
                prefSeats.put(pref, top.getValue());
            }
        }
        mapView.animateResults(prefWinners, prefSeats);
    }

    private String getPrefectureName(String dName) {
        if (dName.length() >= 3) {
            String sub = dName.substring(0, 3);
            if(sub.endsWith("県") || sub.endsWith("都") || sub.endsWith("府")) return sub;
        }
        return dName.substring(0, 2);
    }
}
