package jp.el;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class ElectionApp extends Application {

    private Stage stage;
    private Scene scene;
    private ElectionData data; // データはここで一つだけ持つ

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;

        // 1. データのロード
        this.data = new ElectionData();

        // 2. 最初の画面（タイトル）を表示するためのダミーRootを作成
        StackPane root = new StackPane();
        this.scene = new Scene(root); // サイズはあとで最大化される
        this.scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        // 3. タイトル画面へ遷移
        showTitleScreen();

        // 4. ステージ設定
        stage.setTitle("衆議院選挙シミュレーター 2026");
        stage.setScene(scene);
        stage.setMaximized(true); // ★フルスクリーン
        stage.show();
    }

    // タイトル画面を表示
    public void showTitleScreen() {
        // Viewを作成し、ボタンが押されたときの次の行き先(Action)を渡す
        TitleView view = new TitleView(
                () -> showSimulationScreen(), // スタートボタンの動作
                () -> showListScreen()        // 一覧ボタンの動作
        );
        scene.setRoot(view.getView());
    }

    // シミュレーション画面を表示
    public void showSimulationScreen() {
        ResultView view = new ResultView(
                data,
                true, // isSimulationMode = true
                () -> showTitleScreen() // 戻るボタンの動作
        );
        scene.setRoot(view.getView());
    }

    // 一覧画面を表示
    public void showListScreen() {
        ResultView view = new ResultView(
                data,
                false, // isSimulationMode = false (一覧モード)
                () -> showTitleScreen() // 戻るボタンの動作
        );
        scene.setRoot(view.getView());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
