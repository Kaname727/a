package jp.el;

public class Party {
    private String name;
    private String ideology;    // 新規追加
    private int popularity;     // 新規追加
    private String description; // 新規追加
    private int seats;

    // JSON読み込み用コンストラクタ
    public Party(String name, String ideology, int popularity, String description) {
        this.name = name;
        this.ideology = ideology;
        this.popularity = popularity;
        this.description = description;
        this.seats = 0;
    }

    // 後方互換用コンストラクタ
    public Party(String name) {
        this(name, "不明", 10, "データがありません");
    }

    public void addSeat() { this.seats++; }
    public void reset() { this.seats = 0; }

    public String getName() { return name; }
    public String getIdeology() { return ideology; }
    public int getPopularity() { return popularity; }
    public String getDescription() { return description; }
    public int getSeats() { return seats; }
}