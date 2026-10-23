package wartech.core;

public enum MatchState {
    IN_PROGRESS(0),
    VICTORY(1),
    DEFEAT(2);

    public final int code;

    MatchState(int code) {
        this.code = code;
    }

    public static MatchState fromCode(int code) {
        return switch (code) {
            case 1 -> VICTORY;
            case 2 -> DEFEAT;
            default -> IN_PROGRESS;
        };
    }
}
