package jp.el;

public class Candidate {
    private String name;
    private Party party;
    private int votes;

    public Candidate(String name, Party party) {
        this.name = name;
        this.party = party;
        this.votes = 0;
    }

    public void addVote() { this.votes++; }
    public void resetVotes() { this.votes = 0; }
    public String getName() { return name; }
    public Party getParty() { return party; }
    public int getVotes() { return votes; }
}