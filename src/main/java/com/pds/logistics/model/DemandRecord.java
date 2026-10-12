package com.pds.logistics.model;

import java.time.LocalDate;

/**
 * Demand record: required grain quantity at a demand node (FPS / Distribution Centre).
 */
public class DemandRecord {

    private int        id;
    private Location   location;
    private GrainType  grainType;
    private double     requiredMT;       // MT required in this period
    private double     fulfilledMT;      // filled by optimizer
    private LocalDate  periodStart;
    private LocalDate  periodEnd;
    private int        beneficiaryCount; // number of BPL/AAY households
    private String     remarks;

    public DemandRecord() {}

    public DemandRecord(int id, Location location, GrainType grainType,
                        double requiredMT, LocalDate periodStart, LocalDate periodEnd,
                        int beneficiaryCount) {
        this.id               = id;
        this.location         = location;
        this.grainType        = grainType;
        this.requiredMT       = requiredMT;
        this.periodStart      = periodStart;
        this.periodEnd        = periodEnd;
        this.beneficiaryCount = beneficiaryCount;
        this.fulfilledMT      = 0.0;
    }

    public double getUnmetDemandMT() {
        return Math.max(0, requiredMT - fulfilledMT);
    }

    public double getFulfillmentRatio() {
        if (requiredMT <= 0) return 1.0;
        return Math.min(1.0, fulfilledMT / requiredMT);
    }

    // ─── Getters & Setters ────────────────────────────────────────────────────

    public int       getId()          { return id; }
    public void      setId(int id)    { this.id = id; }

    public Location  getLocation()                  { return location; }
    public void      setLocation(Location location) { this.location = location; }

    public GrainType getGrainType()                    { return grainType; }
    public void      setGrainType(GrainType grainType) { this.grainType = grainType; }

    public double    getRequiredMT()                    { return requiredMT; }
    public void      setRequiredMT(double requiredMT)   { this.requiredMT = requiredMT; }

    public double    getFulfilledMT()                     { return fulfilledMT; }
    public void      setFulfilledMT(double fulfilledMT)   { this.fulfilledMT = fulfilledMT; }

    public LocalDate getPeriodStart()                       { return periodStart; }
    public void      setPeriodStart(LocalDate periodStart)  { this.periodStart = periodStart; }

    public LocalDate getPeriodEnd()                     { return periodEnd; }
    public void      setPeriodEnd(LocalDate periodEnd)  { this.periodEnd = periodEnd; }

    public int       getBeneficiaryCount()                        { return beneficiaryCount; }
    public void      setBeneficiaryCount(int beneficiaryCount)    { this.beneficiaryCount = beneficiaryCount; }

    public String    getRemarks()                { return remarks; }
    public void      setRemarks(String remarks)  { this.remarks = remarks; }

    @Override
    public String toString() {
        return "Demand[%s | %s | req=%.1f MT, filled=%.1f MT]"
                .formatted(location.getCode(), grainType, requiredMT, fulfilledMT);
    }
}
