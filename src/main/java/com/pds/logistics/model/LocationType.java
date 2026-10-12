package com.pds.logistics.model;

/**
 * Enumerates the types of nodes in the PDS logistics network.
 */
public enum LocationType {
    /** FCI / Central godown – acts as supply source */
    DEPOT("Depot / FCI Godown"),
    /** State / District buffer warehouse */
    WAREHOUSE("Buffer Warehouse"),
    /** Taluka-level distribution centre */
    DISTRIBUTION_CENTER("Distribution Centre"),
    /** Fair Price Shop – final demand node */
    FAIR_PRICE_SHOP("Fair Price Shop"),
    /** Internal super-source node for algorithm */
    SUPER_SOURCE("Super Source"),
    /** Internal super-sink node for algorithm */
    SUPER_SINK("Super Sink");

    private final String displayName;

    LocationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
