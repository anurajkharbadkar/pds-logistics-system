package com.pds.logistics.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Summarizes one complete optimization run produced by the MCMF engine.
 * Persisted in the database for history and comparison.
 */
public class OptimizationResult {

    private int                   id;
    private String                scenarioName;
    private LocalDateTime         runTimestamp;
    private GrainType             grainType;
    private double                totalSupplyMT;
    private double                totalDemandMT;
    private double                totalTransportedMT;
    private double                totalCost;             // ₹
    private double                fulfilledDemandMT;
    private double                unmetDemandMT;
    private double                networkEfficiencyPct;  // fulfilledDemand / totalDemand × 100
    private long                  solveDurationMs;
    private String                algorithmUsed;         // e.g. "MCMF-SSP-Dijkstra"
    private String                status;                // SUCCESS / PARTIAL / INFEASIBLE
    private String                notes;
    private List<AllocationResult> allocations = new ArrayList<>();

    public OptimizationResult() {
        this.runTimestamp  = LocalDateTime.now();
        this.algorithmUsed = "MCMF-SSP-Dijkstra";
        this.status        = "SUCCESS";
    }

    public void computeSummary() {
        this.totalTransportedMT  = allocations.stream()
                .mapToDouble(AllocationResult::getAllocatedMT).sum();
        this.totalCost           = allocations.stream()
                .mapToDouble(AllocationResult::getCostContribution).sum();
        this.networkEfficiencyPct = (totalDemandMT > 0)
                ? (fulfilledDemandMT / totalDemandMT) * 100.0
                : 0.0;
    }

    // ─── Getters & Setters ────────────────────────────────────────────────────

    public int            getId()          { return id; }
    public void           setId(int id)    { this.id = id; }

    public String         getScenarioName()                    { return scenarioName; }
    public void           setScenarioName(String scenarioName) { this.scenarioName = scenarioName; }

    public LocalDateTime  getRunTimestamp()                           { return runTimestamp; }
    public void           setRunTimestamp(LocalDateTime runTimestamp) { this.runTimestamp = runTimestamp; }

    public GrainType      getGrainType()                    { return grainType; }
    public void           setGrainType(GrainType grainType) { this.grainType = grainType; }

    public double         getTotalSupplyMT()                      { return totalSupplyMT; }
    public void           setTotalSupplyMT(double totalSupplyMT)  { this.totalSupplyMT = totalSupplyMT; }

    public double         getTotalDemandMT()                      { return totalDemandMT; }
    public void           setTotalDemandMT(double totalDemandMT)  { this.totalDemandMT = totalDemandMT; }

    public double         getTotalTransportedMT()                           { return totalTransportedMT; }
    public void           setTotalTransportedMT(double totalTransportedMT) { this.totalTransportedMT = totalTransportedMT; }

    public double         getTotalCost()                    { return totalCost; }
    public void           setTotalCost(double totalCost)    { this.totalCost = totalCost; }

    public double         getFulfilledDemandMT()                          { return fulfilledDemandMT; }
    public void           setFulfilledDemandMT(double fulfilledDemandMT) { this.fulfilledDemandMT = fulfilledDemandMT; }

    public double         getUnmetDemandMT()                        { return unmetDemandMT; }
    public void           setUnmetDemandMT(double unmetDemandMT)    { this.unmetDemandMT = unmetDemandMT; }

    public double         getNetworkEfficiencyPct()                             { return networkEfficiencyPct; }
    public void           setNetworkEfficiencyPct(double networkEfficiencyPct)  { this.networkEfficiencyPct = networkEfficiencyPct; }

    public long           getSolveDurationMs()                        { return solveDurationMs; }
    public void           setSolveDurationMs(long solveDurationMs)    { this.solveDurationMs = solveDurationMs; }

    public String         getAlgorithmUsed()                       { return algorithmUsed; }
    public void           setAlgorithmUsed(String algorithmUsed)   { this.algorithmUsed = algorithmUsed; }

    public String         getStatus()                { return status; }
    public void           setStatus(String status)  { this.status = status; }

    public String         getNotes()               { return notes; }
    public void           setNotes(String notes)   { this.notes = notes; }

    public List<AllocationResult> getAllocations()                               { return allocations; }
    public void                   setAllocations(List<AllocationResult> allocs)  { this.allocations = allocs; }
    public void                   addAllocation(AllocationResult a)              { this.allocations.add(a); }

    @Override
    public String toString() {
        return "OptResult[%s | %s | transported=%.1f MT | cost=₹%.2f | eff=%.1f%%]"
                .formatted(scenarioName, grainType,
                           totalTransportedMT, totalCost, networkEfficiencyPct);
    }
}
