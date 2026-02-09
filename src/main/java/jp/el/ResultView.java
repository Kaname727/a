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
    // ★追加
    private PrefectureDetailView detailView;

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
        // 2. 地図 (mapWrapper) の構築部分を修正
        mapView = new JapanMapView();

        // ★追加: 詳細パネルの作成 (最初は非表示)
        detailView = new PrefectureDetailView(() -> detailView.setVisible(false));
        detailView.setVisible(false);

        // ★追加: 地図クリック時の動作設定
        mapView.setOnPrefectureClick(prefName -> {
            // データを取得
            PrefectureData pData = data.getPrefectureByName(prefName);
            if (pData != null) {
                // パネルにデータをセットして表示
                detailView.setData(pData);
                detailView.setVisible(true);
                detailView.toFront(); // 最前面へ
            }
        });

        // 地図上の議席数表示切替チェックボックス
        CheckBox labelCheck = new CheckBox("議席数を表示");
        labelCheck.setSelected(true);
        labelCheck.getStyleClass().add("map-toggle");
        labelCheck.setOnAction(e -> mapView.setLabelsVisible(labelCheck.isSelected()));

        // 地図とチェックボックスを重ねる
        mapWrapper = new StackPane(mapView, labelCheck, detailView);
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
    // 例: 全政党の組み合わせで協力度を表示する
    public void printCoalitionMatrix(List<Party> parties) {
        System.out.println("--- 政党間 協力度マトリクス ---");

        for (Party p1 : parties) {
            for (Party p2 : parties) {
                if (p1 == p2) continue; // 自分自身はスキップ

                double score = p1.calculateCooperation(p2);

                // 協力度が70%以上なら「連立の可能性あり」
                if (score >= 70.0) {
                    System.out.printf("🤝 %s と %s は仲が良いです (親和性: %.1f%%)\n",
                            p1.getName(), p2.getName(), score);
                } else if (score <= 30.0) {
                    System.out.printf("⚔️ %s と %s は対立しています (親和性: %.1f%%)\n",
                            p1.getName(), p2.getName(), score);
                }
            }
        }
    }

    private void switchView(javafx.scene.Node show, javafx.scene.Node... hides) {
        show.setVisible(true);
        for(javafx.scene.Node h : hides) h.setVisible(false);
    }

    // ResultView.java

    private void runSimulation() {
        Random rand = new Random();

        // 1. リセット
        data.getParties().forEach(Party::reset);
        data.getDistricts().forEach(d -> d.getCandidates().forEach(Candidate::resetVotes));

        // 2. 選挙区ごとに投票
        for (District d : data.getDistricts()) {
            // 都道府県データを取得
            PrefectureData pref = data.getPrefectureData(d.getName());

            // データがない場合のデフォルト値（全国平均など）
            double primaryInd = (pref != null) ? pref.getPrimary() : 4.0;
            double secondaryInd = (pref != null) ? pref.getSecondary() : 25.0;
            double tertiaryInd = (pref != null) ? pref.getTertiary() : 71.0;
            int income = (pref != null) ? pref.getIncome() : 300;
            double elderly = (pref != null) ? pref.getElderly() : 29.0;

            // 投票数（3000〜6000票）
            int totalVoters = 3000 + rand.nextInt(3000);
            List<Candidate> candidates = d.getCandidates();

            int totalWeight = 0;
            int[] weights = new int[candidates.size()];

            for (int i = 0; i < candidates.size(); i++) {
                Party p = candidates.get(i).getParty();
                Map<String, Integer> ideology = p.getIdeologies(); // 0~20のスコア

                // --- ★支持率計算ロジック ---
                double score = p.getPopularity(); // 基礎人気

                if (ideology != null) {
                    // 1. 産業構造による補正
                    // 第1次産業が高い -> 保守(安定)・環境にプラス
                    if (primaryInd > 8.0) {
                        score += ideology.getOrDefault("保守", 0) * 0.5;
                        score += ideology.getOrDefault("環境主義", 0) * 0.3;
                    }

                    // 第2次産業が高い -> 積極財政・労働支援にプラス
                    if (secondaryInd > 30.0) {
                        score += ideology.getOrDefault("積極財政", 0) * 0.4;
                        // 労働党など特定の名前へのボーナスも可
                        if (p.getName().contains("労働")) score += 10;
                    }

                    // 第3次産業が高い(都市部) -> リベラル・改革(ポピュリズム)にプラス
                    if (tertiaryInd > 75.0) {
                        score += ideology.getOrDefault("リベラル", 0) * 0.5;
                        score += ideology.getOrDefault("ポピュリズム", 0) * 0.3;
                    }

                    // 2. 年収による補正
                    if (income > 350) { // 高所得地域
                        // 減税(リバタリアン)や保守を好む傾向
                        score += ideology.getOrDefault("リバタリアニズム", 0) * 0.4;
                        score += ideology.getOrDefault("保守", 0) * 0.3;
                    } else { // 低所得地域
                        // 再分配(リベラル)や大きな政府(積極財政)を好む
                        score += ideology.getOrDefault("リベラル", 0) * 0.4;
                        score += ideology.getOrDefault("積極財政", 0) * 0.4;
                    }

                    // 3. 年齢構成による補正
                    if (elderly > 32.0) { // 高齢化地域 -> 保守・ナショナリズム
                        score += ideology.getOrDefault("保守", 0) * 0.6;
                        score += ideology.getOrDefault("ナショナリズム", 0) * 0.3;
                    } else { // 若い地域 -> リベラル・革新
                        score += ideology.getOrDefault("リベラル", 0) * 0.5;
                        score += ideology.getOrDefault("環境主義", 0) * 0.3;
                    }
                }

                // ランダムな揺らぎ (+-15%)
                double randomFactor = 0.85 + (rand.nextDouble() * 0.3);
                int finalWeight = (int)(score * randomFactor * 10); // 整数化

                if (finalWeight <= 0) finalWeight = 1;
                weights[i] = finalWeight;
                totalWeight += finalWeight;
            }

            // 加重抽選（重みに応じて当選確率が決まる）
            for(int v = 0; v < totalVoters; v++) {
                int r = rand.nextInt(totalWeight);
                for (int k = 0; k < candidates.size(); k++) {
                    r -= weights[k];
                    if (r < 0) {
                        candidates.get(k).addVote();
                        break;
                    }
                }
            }

            // 勝者判定
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
