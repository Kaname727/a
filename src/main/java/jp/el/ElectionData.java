package jp.el;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.*;

public class ElectionData {
    private List<Party> parties;
    private List<District> districts;
    private final Set<String> currentLawmakers = new HashSet<>();

    // 都道府県ごとの定数データ
    private static final Map<String, Integer> PREFECTURE_SEATS = new LinkedHashMap<>();
    static {
        PREFECTURE_SEATS.put("北海道", 12); PREFECTURE_SEATS.put("青森県", 3); PREFECTURE_SEATS.put("岩手県", 3);
        PREFECTURE_SEATS.put("宮城県", 5); PREFECTURE_SEATS.put("秋田県", 3); PREFECTURE_SEATS.put("山形県", 3);
        PREFECTURE_SEATS.put("福島県", 4); PREFECTURE_SEATS.put("茨城県", 7); PREFECTURE_SEATS.put("栃木県", 5);
        PREFECTURE_SEATS.put("群馬県", 5); PREFECTURE_SEATS.put("埼玉県", 16); PREFECTURE_SEATS.put("千葉県", 14);
        PREFECTURE_SEATS.put("東京都", 30); PREFECTURE_SEATS.put("神奈川県", 20); PREFECTURE_SEATS.put("新潟県", 5);
        PREFECTURE_SEATS.put("富山県", 3); PREFECTURE_SEATS.put("石川県", 3); PREFECTURE_SEATS.put("福井県", 2);
        PREFECTURE_SEATS.put("山梨県", 2); PREFECTURE_SEATS.put("長野県", 5); PREFECTURE_SEATS.put("岐阜県", 5);
        PREFECTURE_SEATS.put("静岡県", 8); PREFECTURE_SEATS.put("愛知県", 16); PREFECTURE_SEATS.put("三重県", 4);
        PREFECTURE_SEATS.put("滋賀県", 3); PREFECTURE_SEATS.put("京都府", 6); PREFECTURE_SEATS.put("大阪府", 19);
        PREFECTURE_SEATS.put("兵庫県", 12); PREFECTURE_SEATS.put("奈良県", 3); PREFECTURE_SEATS.put("和歌山県", 2);
        PREFECTURE_SEATS.put("鳥取県", 2); PREFECTURE_SEATS.put("島根県", 2); PREFECTURE_SEATS.put("岡山県", 4);
        PREFECTURE_SEATS.put("広島県", 6); PREFECTURE_SEATS.put("山口県", 3); PREFECTURE_SEATS.put("徳島県", 2);
        PREFECTURE_SEATS.put("香川県", 3); PREFECTURE_SEATS.put("愛媛県", 3); PREFECTURE_SEATS.put("高知県", 2);
        PREFECTURE_SEATS.put("福岡県", 11); PREFECTURE_SEATS.put("佐賀県", 2); PREFECTURE_SEATS.put("長崎県", 3);
        PREFECTURE_SEATS.put("熊本県", 4); PREFECTURE_SEATS.put("大分県", 3); PREFECTURE_SEATS.put("宮崎県", 3);
        PREFECTURE_SEATS.put("鹿児島県", 4); PREFECTURE_SEATS.put("沖縄県", 4);
    }

    private static final String[] SURNAMES = {"佐藤", "鈴木", "高橋", "田中", "伊藤", "渡辺", "山本", "中村", "小林", "加藤"};
    private static final String[] NAMES = {"一郎", "次郎", "花子", "美咲", "大輔", "誠", "陽子", "健太", "直人", "由美"};

    // JSONマッピング用DTO
    public static class PartyDTO {
        public String name;
        public String ideology;
        public int popularity;
        public String description;
    }

    public ElectionData() {
        init();
    }

    private void init() {
        parties = new ArrayList<>();
        districts = new ArrayList<>();
        loadCurrentLawmakers();
        ObjectMapper mapper = new ObjectMapper();

        try {
            // 1. JSONから政党読み込み
            InputStream partyStream = getClass().getResourceAsStream("/parties.json");
            if (partyStream != null) {
                List<PartyDTO> dtos = mapper.readValue(partyStream, new TypeReference<List<PartyDTO>>() {});
                for (PartyDTO dto : dtos) {
                    parties.add(new Party(dto.name, dto.ideology, dto.popularity, dto.description));
                }
            } else {
                parties.add(new Party("自由党", "保守", 40, "説明なし"));
                parties.add(new Party("民進党", "リベラル", 30, "説明なし"));
            }

            // 2. 全国選挙区生成
            generateNationwideDistricts();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void generateNationwideDistricts() {
        Random rand = new Random();
        for (Map.Entry<String, Integer> entry : PREFECTURE_SEATS.entrySet()) {
            String prefName = entry.getKey();
            int seats = entry.getValue();

            for (int i = 1; i <= seats; i++) {
                String districtName = prefName + "第" + i + "区";
                List<Candidate> candidates = new ArrayList<>();
                int numCandidates = 2 + rand.nextInt(3);

                List<Party> shuffled = new ArrayList<>(parties);
                Collections.shuffle(shuffled);

                for (int j = 0; j < numCandidates; j++) {
                    if (j >= shuffled.size()) break;
                    candidates.add(new Candidate(generateName(), shuffled.get(j)));
                }
                districts.add(new District(districtName, candidates));
            }
        }
    }

    private String generateName() {
        Random r = new Random();
        for (int attempts = 0; attempts < 1000; attempts++) {
            String name = SURNAMES[r.nextInt(SURNAMES.length)] + " " + NAMES[r.nextInt(NAMES.length)];
            if (!currentLawmakers.contains(name)) {
                return name;
            }
        }
        return "匿名 候補";
    }

    private void loadCurrentLawmakers() {
        try (InputStream stream = getClass().getResourceAsStream("/current_diet_members.txt")) {
            if (stream == null) {
                return;
            }
            Scanner scanner = new Scanner(stream, "UTF-8");
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    currentLawmakers.add(line);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Party> getParties() { return parties; }
    public List<District> getDistricts() { return districts; }
}
