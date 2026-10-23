package wartech.ai;

// Enemy difficulty tiers

public enum Difficulty {
    EASY(1, "RECRUIT", "EASY", "LOW"),
    MEDIUM(2, "VETERAN", "MEDIUM", "MEDIUM"),
    HARD(3, "COMMANDER", "HARD", "MAXIMUM");

    public final int rank; // higher rank is harder, used to gate some actions
    private final String label;
    private final String tier;
    private final String threat;

    Difficulty(int rank, String label, String tier, String threat) {
        this.rank = rank;
        this.label = label;
        this.tier = tier;
        this.threat = threat;
    }

    public boolean atLeast(Difficulty other) {
        return rank >= other.rank;
    }

    public String label() {
        return label;
    }

    public String tier() {
        return tier;
    }

    public String threatProfile() {
        return label + " (" + threat + ")";
    }
}
