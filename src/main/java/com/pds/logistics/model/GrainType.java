package com.pds.logistics.model;

/**
 * Grain commodities managed by the PDS network.
 */
public enum GrainType {
    WHEAT("Wheat", "MT"),
    RICE("Rice", "MT"),
    COARSE_GRAINS("Coarse Grains", "MT"),
    PULSES("Pulses", "MT");

    private final String displayName;
    private final String unit;

    GrainType(String displayName, String unit) {
        this.displayName = displayName;
        this.unit = unit;
    }

    public String getDisplayName() { return displayName; }
    public String getUnit()        { return unit; }

    @Override
    public String toString() { return displayName; }
}
