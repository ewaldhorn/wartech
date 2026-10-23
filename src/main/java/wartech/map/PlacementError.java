package wartech.map;

public enum PlacementError {
    NONE(""),
    OUT_OF_BOUNDS("CANNOT BUILD OFF THE MAP"),
    NOT_BEDROCK("REQUIRES SOLID BEDROCK"),
    CLIFF_BUFFER("TOO CLOSE TO A CLIFF FACE"),
    NO_INFLUENCE("OUTSIDE BASE TERRITORY"),
    TOO_CLOSE("TOO CLOSE TO ANOTHER STRUCTURE"),
    NO_EXIT("NO ROOM FOR THE UNIT EXIT BELOW"),
    UNIT_OCCUPIED("UNIT IN THE WAY");

    private final String label;

    PlacementError(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
