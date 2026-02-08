package jp.el;

import javafx.scene.paint.Color;
import java.util.HashMap;
import java.util.Map;

public class PartyColors {
    private static final Map<String, Color> COLORS = new HashMap<>();

    static {
        COLORS.put("自由党", Color.web("#e74c3c"));
        COLORS.put("民進党", Color.web("#3498db"));
        COLORS.put("未来党", Color.web("#2ecc71"));
        COLORS.put("市民の会", Color.web("#f1c40f"));
        COLORS.put("保守党", Color.web("#8e44ad"));
        COLORS.put("革新党", Color.web("#1abc9c"));
        COLORS.put("緑の党", Color.web("#27ae60"));
        COLORS.put("労働党", Color.web("#e67e22"));
        COLORS.put("無所属", Color.web("#95a5a6"));
    }

    public static Color get(String partyName) {
        return COLORS.getOrDefault(partyName, Color.GRAY);
    }

    // CSS用にHEXコード(#RRGGBB)で取得
    public static String getHex(String partyName) {
        Color c = get(partyName);
        return String.format("#%02x%02x%02x",
                (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255));
    }
}