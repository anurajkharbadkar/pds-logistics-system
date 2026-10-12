package com.pds.logistics.model;

import java.time.LocalDate;

/**
 * Inventory record: quantity of a specific grain at a specific location.
 */
public class InventoryRecord {

    private int         id;
    private Location    location;
    private GrainType   grainType;
    private double      quantityMT;       // current available stock in MT
    private double      allocatedMT;      // committed/reserved quantity
    private LocalDate   reportDate;
    private String      remarks;

    public InventoryRecord() {}

    public InventoryRecord(int id, Location location, GrainType grainType,
                           double quantityMT, LocalDate reportDate) {
        this.id         = id;
        this.location   = location;
        this.grainType  = grainType;
        this.quantityMT = quantityMT;
        this.reportDate = reportDate;
        this.allocatedMT = 0.0;
    }

    /** Effective supply = total stock minus already-allocated quantity */
    public double getEffectiveSupplyMT() {
        return Math.max(0, quantityMT - allocatedMT);
    }

    // ─── Getters & Setters ────────────────────────────────────────────────────

    public int        getId()          { return id; }
    public void       setId(int id)    { this.id = id; }

    public Location   getLocation()                  { return location; }
    public void       setLocation(Location location) { this.location = location; }

    public GrainType  getGrainType()                    { return grainType; }
    public void       setGrainType(GrainType grainType) { this.grainType = grainType; }

    public double     getQuantityMT()                   { return quantityMT; }
    public void       setQuantityMT(double quantityMT)  { this.quantityMT = quantityMT; }

    public double     getAllocatedMT()                    { return allocatedMT; }
    public void       setAllocatedMT(double allocatedMT) { this.allocatedMT = allocatedMT; }

    public LocalDate  getReportDate()                     { return reportDate; }
    public void       setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }

    public String     getRemarks()                { return remarks; }
    public void       setRemarks(String remarks)  { this.remarks = remarks; }

    @Override
    public String toString() {
        return "Inventory[%s | %s | %.1f MT available]"
                .formatted(location.getCode(), grainType, getEffectiveSupplyMT());
    }
}
