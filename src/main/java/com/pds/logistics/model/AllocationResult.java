package com.pds.logistics.model;

/**
 * Represents a single flow allocation on a route segment produced by the MCMF solver.
 * Each instance maps to one directed edge that carries non-zero flow.
 */
public class AllocationResult {

    private int            optimizationRunId;
    private TransportRoute route;
    private double         allocatedMT;       // flow on this edge
    private double         costContribution;  // allocatedMT × unitCost
    private GrainType      grainType;

    public AllocationResult() {}

    public AllocationResult(int optimizationRunId, TransportRoute route,
                            double allocatedMT, GrainType grainType) {
        this.optimizationRunId = optimizationRunId;
        this.route             = route;
        this.allocatedMT       = allocatedMT;
        this.grainType         = grainType;
        this.costContribution  = allocatedMT * route.getUnitCostPerMT();
    }

    // ─── Getters & Setters ────────────────────────────────────────────────────

    public int            getOptimizationRunId()                        { return optimizationRunId; }
    public void           setOptimizationRunId(int optimizationRunId)   { this.optimizationRunId = optimizationRunId; }

    public TransportRoute getRoute()                  { return route; }
    public void           setRoute(TransportRoute r)  { this.route = r; }

    public double         getAllocatedMT()                     { return allocatedMT; }
    public void           setAllocatedMT(double allocatedMT)  { this.allocatedMT = allocatedMT; }

    public double         getCostContribution()                         { return costContribution; }
    public void           setCostContribution(double costContribution)  { this.costContribution = costContribution; }

    public GrainType      getGrainType()                    { return grainType; }
    public void           setGrainType(GrainType grainType) { this.grainType = grainType; }

    @Override
    public String toString() {
        return "Alloc[%s → %s | %.2f MT | ₹%.2f]"
                .formatted(route.getSource().getCode(),
                           route.getDestination().getCode(),
                           allocatedMT, costContribution);
    }
}
