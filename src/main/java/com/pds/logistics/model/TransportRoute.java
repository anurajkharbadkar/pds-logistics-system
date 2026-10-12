package com.pds.logistics.model;

import java.util.Objects;

/**
 * A directed transportation route (edge) between two locations in the PDS network.
 * Stores unit cost (₹/MT), capacity (MT), distance (km), and availability flag.
 */
public class TransportRoute {

    private int      id;
    private Location source;
    private Location destination;
    private double   capacityMT;        // Maximum tonnage that can be transported
    private double   unitCostPerMT;     // ₹ per metric tonne
    private double   distanceKm;
    private String   transportMode;     // ROAD / RAIL / RIVER
    private boolean  active;
    private String   remarks;

    public TransportRoute() {}

    public TransportRoute(int id, Location source, Location destination,
                          double capacityMT, double unitCostPerMT,
                          double distanceKm, String transportMode) {
        this.id            = id;
        this.source        = source;
        this.destination   = destination;
        this.capacityMT    = capacityMT;
        this.unitCostPerMT = unitCostPerMT;
        this.distanceKm    = distanceKm;
        this.transportMode = transportMode;
        this.active        = true;
    }

    // ─── Getters & Setters ────────────────────────────────────────────────────

    public int      getId()          { return id; }
    public void     setId(int id)    { this.id = id; }

    public Location getSource()                 { return source; }
    public void     setSource(Location source)  { this.source = source; }

    public Location getDestination()                    { return destination; }
    public void     setDestination(Location destination){ this.destination = destination; }

    public double   getCapacityMT()                   { return capacityMT; }
    public void     setCapacityMT(double capacityMT)  { this.capacityMT = capacityMT; }

    public double   getUnitCostPerMT()                      { return unitCostPerMT; }
    public void     setUnitCostPerMT(double unitCostPerMT)  { this.unitCostPerMT = unitCostPerMT; }

    public double   getDistanceKm()                   { return distanceKm; }
    public void     setDistanceKm(double distanceKm)  { this.distanceKm = distanceKm; }

    public String   getTransportMode()                        { return transportMode; }
    public void     setTransportMode(String transportMode)    { this.transportMode = transportMode; }

    public boolean  isActive()               { return active; }
    public void     setActive(boolean active){ this.active = active; }

    public String   getRemarks()                  { return remarks; }
    public void     setRemarks(String remarks)    { this.remarks = remarks; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransportRoute that)) return false;
        return id == that.id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "Route[%d] %s → %s (cap=%.1f MT, cost=₹%.2f/MT)"
                .formatted(id, source.getCode(), destination.getCode(),
                           capacityMT, unitCostPerMT);
    }
}
