package jp.el;
import java.util.Comparator;
import java.util.List;

public class District {
    private String name;
    private List<Candidate> candidates;

    public District(String name, List<Candidate> candidates) {
        this.name = name;
        this.candidates = candidates;
    }

    public Candidate getWinner() {
        return candidates.stream()
                .max(Comparator.comparingInt(Candidate::getVotes))
                .orElse(null);
    }

    public String getName() { return name; }
    public List<Candidate> getCandidates() { return candidates; }
}