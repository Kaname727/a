package jp.el;

public class PrefectureData {
    private String name;
    private int income;      // 1人あたり所得(万円)
    private double primary;  // 第1次産業(%)
    private double secondary;// 第2次産業(%)
    private double tertiary; // 第3次産業(%)
    private double elderly;  // 高齢化率(%)

    // デフォルトコンストラクタ（Jackson用）
    public PrefectureData() {}

    public String getName() { return name; }
    public int getIncome() { return income; }
    public double getPrimary() { return primary; }
    public double getSecondary() { return secondary; }
    public double getTertiary() { return tertiary; }
    public double getElderly() { return elderly; }
}