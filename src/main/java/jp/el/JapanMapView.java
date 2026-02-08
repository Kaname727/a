package jp.el;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.util.*;

public class JapanMapView extends Pane {
    private final Map<String, SVGPath> prefectureShapes = new HashMap<>();
    private final Pane labelLayer = new Pane();
    private final Group contentGroup = new Group();

    public JapanMapView() {
        this.getStyleClass().add("map-container");
        initializeMap();
    }

    private void initializeMap() {
        Map<String, String> paths = GeoJsonLoader.loadJapanMap();
        if (paths.isEmpty()) paths = JapanMapData.PATHS;

        for (Map.Entry<String, String> entry : paths.entrySet()) {
            String prefName = entry.getKey();
            SVGPath path = new SVGPath();
            path.setContent(entry.getValue());
            path.getStyleClass().add("map-path");

            path.setFill(Color.WHITE);
            path.setStroke(Color.LIGHTGRAY);
            path.setStrokeWidth(0.5);

            Tooltip.install(path, new Tooltip(prefName));

            path.setOnMouseEntered(e -> {
                path.setStroke(Color.BLACK);
                path.setStrokeWidth(1.0);
                path.toFront();
            });
            path.setOnMouseExited(e -> {
                path.setStroke(Color.LIGHTGRAY);
                path.setStrokeWidth(0.5);
            });

            contentGroup.getChildren().add(path);
            prefectureShapes.put(prefName, path);
        }

        labelLayer.setMouseTransparent(true);
        contentGroup.getChildren().add(labelLayer);
        this.getChildren().add(contentGroup);

        javafx.application.Platform.runLater(this::fitMapToView);
    }

    private void fitMapToView() {
        Bounds bounds = contentGroup.getBoundsInParent();
        if (bounds.getWidth() == 0) return;

        double viewWidth = 800;
        double viewHeight = 600;
        double scale = Math.min(viewWidth / bounds.getWidth(), viewHeight / bounds.getHeight()) * 0.95;

        contentGroup.setScaleX(scale);
        contentGroup.setScaleY(scale);

        double scaledW = bounds.getWidth() * scale;
        contentGroup.setLayoutX((viewWidth - scaledW) / 2 - bounds.getMinX() * scale);
        contentGroup.setLayoutY(20 - bounds.getMinY() * scale);
    }

    // ★追加: ラベルの表示・非表示を切り替えるメソッド
    public void setLabelsVisible(boolean visible) {
        labelLayer.setVisible(visible);
    }

    public void animateResults(Map<String, String> prefWinners, Map<String, Integer> prefSeats) {
        labelLayer.getChildren().clear();

        List<Map.Entry<String, SVGPath>> sorted = new ArrayList<>(prefectureShapes.entrySet());
        sorted.sort(Comparator.comparingDouble(e -> e.getValue().getBoundsInParent().getMinY()));

        Timeline timeline = new Timeline();
        double delay = 0;

        for (Map.Entry<String, SVGPath> entry : sorted) {
            String prefName = entry.getKey();
            SVGPath shape = entry.getValue();

            String key = matchKey(prefName, prefWinners.keySet());

            if (key != null) {
                String party = prefWinners.get(key);
                int seats = prefSeats.get(key);
                Color color = PartyColors.get(party);

                KeyFrame kf = new KeyFrame(Duration.millis(delay), e -> {
                    shape.setFill(color);
                    addLabel(shape, String.valueOf(seats));
                });
                timeline.getKeyFrames().add(kf);
                delay += 120;
            } else {
                shape.setFill(Color.WHITE);
            }
        }
        timeline.play();
    }

    private void addLabel(SVGPath shape, String text) {
        Bounds b = shape.getBoundsInParent();
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        label.setTextFill(Color.WHITE);
        label.setEffect(new DropShadow(2, Color.BLACK));

        label.setLayoutX(b.getMinX() + b.getWidth() / 2 - 10);
        label.setLayoutY(b.getMinY() + b.getHeight() / 2 - 10);
        labelLayer.getChildren().add(label);
    }

    private String matchKey(String target, Set<String> keys) {
        if (keys.contains(target)) return target;
        for (String k : keys) {
            if (k.startsWith(target) || target.startsWith(k)) return k;
        }
        return null;
    }
}