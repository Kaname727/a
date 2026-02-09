package jp.el;

import java.util.Map;

public class Party {
    private String name;
    private String ideology;
    private int popularity;
    private String description;
    private Map<String, Integer> ideologies;
    private int seats;

    // ★追加: 与党かどうかを判定するフラグ
    private boolean isGovernment;

    public Party(String name, String ideology, int popularity, String description, Map<String, Integer> ideologies) {
        this.name = name;
        this.ideology = ideology;
        this.popularity = popularity;
        this.description = description;
        this.ideologies = ideologies;
        this.seats = 0;
        this.isGovernment = false;
    }

    // 協力度計算（既存のまま）
    public double calculateCooperation(Party other) {
        if (this.ideologies == null || other.getIdeologies() == null) {
            return 0.0;
        }
        String[] axes = {
                "保守", "リベラル", "ポピュリズム", "リバタリアニズム",
                "環境主義", "積極財政", "緊縮財政", "ナショナリズム"
        };
        double sumSquaredDiff = 0.0;
        for (String axis : axes) {
            int val1 = this.ideologies.getOrDefault(axis, 0);
            int val2 = other.getIdeologies().getOrDefault(axis, 0);
            sumSquaredDiff += Math.pow(val1 - val2, 2);
        }
        double distance = Math.sqrt(sumSquaredDiff);
        double maxDistance = 56.5;
        double score = 100.0 * (1.0 - (distance / maxDistance));
        return Math.max(0.0, score);
    }

    public Party(String name) {
        this(name, "不明", 10, "データがありません", null);
    }

    public void addSeat() { this.seats++; }

    // リセット時に与党フラグも戻す
    public void reset() {
        this.seats = 0;
        this.isGovernment = false;
    }

    public String getName() { return name; }
    public String getIdeology() { return ideology; }
    public int getPopularity() { return popularity; }
    public String getDescription() { return description; }
    public Map<String, Integer> getIdeologies() { return ideologies; }
    public int getSeats() { return seats; }

    // ★追加: 与党フラグのGetter/Setter
    public boolean isGovernment() { return isGovernment; }
    public void setGovernment(boolean isGovernment) { this.isGovernment = isGovernment; }
}