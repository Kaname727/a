package jp.el;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class DistrictCell extends ListCell<District> {
    private final boolean isSimulationMode;

    public DistrictCell(boolean isSimulationMode) {
        this.isSimulationMode = isSimulationMode;
    }

    @Override
    protected void updateItem(District d, boolean empty) {
        super.updateItem(d, empty);

        if (empty || d == null) {
            setGraphic(null);
            setStyle("-fx-background-color: transparent;");
        } else {
            VBox card = new VBox(10);
            card.getStyleClass().add("district-card"); // CSS適用

            Label dName = new Label("📍 " + d.getName());
            dName.setFont(Font.font("System", FontWeight.BOLD, 16));
            dName.setTextFill(Color.web("#2c3e50"));

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(5);
            grid.setAlignment(Pos.CENTER_LEFT);

            // カラム定義
            ColumnConstraints col1 = new ColumnConstraints(); col1.setPrefWidth(180);
            ColumnConstraints col2 = new ColumnConstraints(); col2.setHgrow(Priority.ALWAYS);
            ColumnConstraints col3 = new ColumnConstraints(); col3.setPrefWidth(80);
            grid.getColumnConstraints().addAll(col1, col2, col3);

            Candidate winner = isSimulationMode ? d.getWinner() : null;
            int totalVotes = d.getCandidates().stream().mapToInt(Candidate::getVotes).sum();
            if (totalVotes == 0) totalVotes = 1;

            int row = 0;
            for (Candidate c : d.getCandidates()) {
                // 名前
                String text = c.getName() + " (" + c.getParty().getName() + ")";
                Label nameLabel = new Label(text);
                nameLabel.setTextFill(Color.BLACK); // 明示的に黒

                if (isSimulationMode && c == winner) {
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #e74c3c;");
                    nameLabel.setText("💮 " + text);
                }
                grid.add(nameLabel, 0, row);

                if (isSimulationMode) {
                    // バー
                    ProgressBar pb = new ProgressBar((double) c.getVotes() / totalVotes);
                    pb.setMaxWidth(Double.MAX_VALUE);

                    // PartyColorsクラスを使って色を取得
                    String hexColor = PartyColors.getHex(c.getParty().getName());

                    if (c == winner) {
                        pb.setStyle("-fx-accent: " + hexColor + ";");
                    } else {
                        pb.setStyle("-fx-accent: #95a5a6;");
                    }
                    grid.add(pb, 1, row);

                    // 票数
                    Label v = new Label(c.getVotes() + "票");
                    v.setAlignment(Pos.CENTER_RIGHT);
                    grid.add(v, 2, row);
                }
                row++;
            }
            card.getChildren().addAll(dName, new Separator(), grid);
            setGraphic(card);
            setStyle("-fx-background-color: transparent;");
        }
    }
}