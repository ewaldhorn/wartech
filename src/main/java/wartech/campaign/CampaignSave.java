package wartech.campaign;

// Campaign persistence and versioning

import wartech.core.MatchState;

public final class CampaignSave {
    public static final String SAVE_KEY = "wartech_rts_campaign_save";
    public static final String VERSION_STAMP_KEY = "wartech_rts_save_version";
    public static final String CURRENT_VERSION = "2";

    private CampaignSave() {
    }

    public static boolean shouldDiscardSave(String savedVersion, boolean forceReset) {
        return forceReset || !CURRENT_VERSION.equals(savedVersion);
    }

    public static boolean shouldPersist(boolean campaignActive, MatchState matchState) {
        return campaignActive && matchState == MatchState.VICTORY;
    }

    public static int parseSave(String raw) {
        if (raw == null) return -1;
        int len = raw.length();
        if (len == 0) return -1;
        long value = 0;
        for (int i = 0; i < len; i++) {
            char c = raw.charAt(i);
            if (c < '0' || c > '9') return -1;
            value = value * 10 + (c - '0');
            if (value > 0xFFF) return -1;
        }
        return (int) value;
    }

    public static boolean hasQueryFlag(String search, String flag) {
        if (search == null || search.length() == 0) return false;
        int start = search.charAt(0) == '?' ? 1 : 0;
        while (start <= search.length()) {
            int amp = search.indexOf('&', start);
            int end = amp < 0 ? search.length() : amp;
            int eq = search.indexOf('=', start);
            int keyEnd = (eq < 0 || eq > end) ? end : eq;
            if (keyEnd - start == flag.length() && search.regionMatches(start, flag, 0, flag.length())) {
                return true;
            }
            if (amp < 0) return false;
            start = amp + 1;
        }
        return false;
    }
}
