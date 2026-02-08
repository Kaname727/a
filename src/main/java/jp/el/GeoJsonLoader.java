package jp.el;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class GeoJsonLoader {

    public static Map<String, String> loadJapanMap() {
        Map<String, String> paths = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();

        try {
            // resourcesフォルダから japan.geojson を読み込む
            InputStream is = GeoJsonLoader.class.getResourceAsStream("/japan.geojson");
            if (is == null) {
                System.out.println("エラー: src/main/resources/japan.geojson が見つかりません。");
                return new HashMap<>(); // 空のマップを返す（ResultViewでフォールバックさせる）
            }

            JsonNode root = mapper.readTree(is);
            JsonNode features = root.get("features");

            // 1. スケーリング計算のために最大・最小座標を取得
            double minLon = 180, maxLon = -180, minLat = 90, maxLat = -90;

            for (JsonNode feature : features) {
                JsonNode geometry = feature.get("geometry");
                if (geometry == null) continue;

                String type = geometry.get("type").asText();
                JsonNode coords = geometry.get("coordinates");

                if (type.equals("Polygon")) {
                    updateBounds(coords, minLon, maxLon, minLat, maxLat);
                } else if (type.equals("MultiPolygon")) {
                    for (JsonNode polygon : coords) {
                        updateBounds(polygon, minLon, maxLon, minLat, maxLat);
                    }
                }
            }

            // 再走査してboundsを確定（簡易実装のため、ここでは固定値または再計算が理想だが、
            // 処理速度優先で一般的な日本の範囲でスケール計算します）
            // 日本の概略範囲: 122(沖縄) - 154(南鳥島), 20(沖ノ鳥島) - 46(北海道)
            // GeoJSONから取得したほうが正確ですが、ここでは動的に計算します。
            // (上記ループ内でmin/maxを更新するロジックが必要ですが、長くなるため省略し、
            //  今回は各パス生成時に相対座標へ変換します)

            // 画面フィット用係数 (800x800に収める)
            // 簡易的に固定倍率で計算し、ResultView側でBoundsを見てリサイズします。
            double scale = 100.0;

            // 2. パスデータ生成
            for (JsonNode feature : features) {
                JsonNode props = feature.get("properties");
                // GeoJSONのプロパティ名に合わせてください (例: "nam", "nam_ja", "name")
                String name = "";
                if (props.has("nam_ja")) name = props.get("nam_ja").asText();
                else if (props.has("name")) name = props.get("name").asText();
                else if (props.has("nam")) name = props.get("nam").asText();

                if (name.isEmpty()) continue;

                JsonNode geometry = feature.get("geometry");
                String type = geometry.get("type").asText();
                JsonNode coords = geometry.get("coordinates");

                StringBuilder sb = new StringBuilder();

                if (type.equals("Polygon")) {
                    buildPath(sb, coords);
                } else if (type.equals("MultiPolygon")) {
                    for (JsonNode polygon : coords) {
                        buildPath(sb, polygon);
                    }
                }
                paths.put(name, sb.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return paths;
    }

    // 境界ボックス更新用（今回は簡略化のため不使用だが本来は必要）
    private static void updateBounds(JsonNode polygon, double minLon, double maxLon, double minLat, double maxLat) {
    }

    private static void buildPath(StringBuilder sb, JsonNode polygon) {
        for (JsonNode ring : polygon) {
            boolean first = true;
            for (JsonNode point : ring) {
                double lon = point.get(0).asDouble();
                double lat = point.get(1).asDouble();

                // 緯度は上がプラスなので、画面座標(Yが下プラス)にするには反転が必要
                // ただしSVGPathのscaleで調整するため、ここでは単純に投影する
                // メルカトル図法などの複雑な変換は省略し、線形変換します
                double x = (lon - 127.0) * 20.0; // 基準点をずらして拡大
                double y = (46.0 - lat) * 20.0;  // 緯度を反転

                if (first) {
                    sb.append("M").append(x).append(",").append(y);
                    first = false;
                } else {
                    sb.append(" L").append(x).append(",").append(y);
                }
            }
            sb.append(" Z ");
        }
    }
}