package com.pds.logistics.algorithm;

import com.pds.logistics.model.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * Defines a what-if scenario by applying modifications to the base network
 * before handing it to the MCMF solver.
 */
public class Scenario {

    public enum ScenarioType {
        BASELINE,
        SUPPLY_CHANGE,
        DEMAND_SURGE,
        ROUTE_FAILURE,
        COST_CHANGE,
        CAPACITY_CHANGE
    }

    private final String       name;
    private final ScenarioType type;
    private final String       description;

    // Modification parameters (each can be null if not applicable)

    /** locationId → new supply MT (for SUPPLY_CHANGE) */
    private Map<Integer, Double> supplyOverrides;

    /** locationId → new demand MT (for DEMAND_SURGE) */
    private Map<Integer, Double> demandOverrides;

    /** routeIds to disable (for ROUTE_FAILURE) */
    private List<Integer> disabledRouteIds;

    /** routeId → new cost per MT (for COST_CHANGE) */
    private Map<Integer, Double> costOverrides;

    /** routeId → new capacity MT (for CAPACITY_CHANGE) */
    private Map<Integer, Double> capacityOverrides;

    /** Global supply multiplier (e.g. 0.8 = 20% supply drop) */
    private double supplyMultiplier = 1.0;

    /** Global demand multiplier (e.g. 1.3 = 30% demand surge) */
    private double demandMultiplier = 1.0;

    public Scenario(String name, ScenarioType type, String description) {
        this.name        = name;
        this.type        = type;
        this.description = description;
    }

    /**
     * Applies this scenario's modifications to copies of the supply, demand,
     * and route lists, leaving the originals unchanged.
     */
    public ScenarioInputs apply(List<InventoryRecord>  baseSupplies,
                                List<DemandRecord>     baseDemands,
                                List<TransportRoute>   baseRoutes) {

        // Deep-copy supplies with modifications
        List<InventoryRecord> modSupplies = new ArrayList<>();
        for (InventoryRecord inv : baseSupplies) {
            InventoryRecord copy = shallowCopyInventory(inv);
            if (supplyOverrides != null && supplyOverrides.containsKey(inv.getLocation().getId())) {
                copy.setQuantityMT(supplyOverrides.get(inv.getLocation().getId()));
            } else {
                copy.setQuantityMT(inv.getQuantityMT() * supplyMultiplier);
            }
            modSupplies.add(copy);
        }

        // Deep-copy demands with modifications
        List<DemandRecord> modDemands = new ArrayList<>();
        for (DemandRecord dem : baseDemands) {
            DemandRecord copy = shallowCopyDemand(dem);
            if (demandOverrides != null && demandOverrides.containsKey(dem.getLocation().getId())) {
                copy.setRequiredMT(demandOverrides.get(dem.getLocation().getId()));
            } else {
                copy.setRequiredMT(dem.getRequiredMT() * demandMultiplier);
            }
            modDemands.add(copy);
        }

        // Deep-copy routes with modifications
        List<TransportRoute> modRoutes = new ArrayList<>();
        for (TransportRoute route : baseRoutes) {
            TransportRoute copy = shallowCopyRoute(route);

            // Disable failed routes
            if (disabledRouteIds != null && disabledRouteIds.contains(route.getId())) {
                copy.setActive(false);
            }
            // Apply cost overrides
            if (costOverrides != null && costOverrides.containsKey(route.getId())) {
                copy.setUnitCostPerMT(costOverrides.get(route.getId()));
            }
            // Apply capacity overrides
            if (capacityOverrides != null && capacityOverrides.containsKey(route.getId())) {
                copy.setCapacityMT(capacityOverrides.get(route.getId()));
            }
            modRoutes.add(copy);
        }

        return new ScenarioInputs(name, modSupplies, modDemands, modRoutes);
    }

    // ─── Shallow copy helpers ─────────────────────────────────────────────────

    private InventoryRecord shallowCopyInventory(InventoryRecord orig) {
        InventoryRecord c = new InventoryRecord();
        c.setId(orig.getId());
        c.setLocation(orig.getLocation());
        c.setGrainType(orig.getGrainType());
        c.setQuantityMT(orig.getQuantityMT());
        c.setAllocatedMT(orig.getAllocatedMT());
        c.setReportDate(orig.getReportDate());
        return c;
    }

    private DemandRecord shallowCopyDemand(DemandRecord orig) {
        DemandRecord c = new DemandRecord();
        c.setId(orig.getId());
        c.setLocation(orig.getLocation());
        c.setGrainType(orig.getGrainType());
        c.setRequiredMT(orig.getRequiredMT());
        c.setFulfilledMT(0);
        c.setPeriodStart(orig.getPeriodStart());
        c.setPeriodEnd(orig.getPeriodEnd());
        c.setBeneficiaryCount(orig.getBeneficiaryCount());
        return c;
    }

    private TransportRoute shallowCopyRoute(TransportRoute orig) {
        TransportRoute c = new TransportRoute();
        c.setId(orig.getId());
        c.setSource(orig.getSource());
        c.setDestination(orig.getDestination());
        c.setCapacityMT(orig.getCapacityMT());
        c.setUnitCostPerMT(orig.getUnitCostPerMT());
        c.setDistanceKm(orig.getDistanceKm());
        c.setTransportMode(orig.getTransportMode());
        c.setActive(orig.isActive());
        return c;
    }

    // ─── Getters & Setters ────────────────────────────────────────────────────

    public String       getName()        { return name; }
    public ScenarioType getType()        { return type; }
    public String       getDescription() { return description; }

    public void setSupplyOverrides(Map<Integer, Double> m)   { this.supplyOverrides = m; }
    public void setDemandOverrides(Map<Integer, Double> m)   { this.demandOverrides = m; }
    public void setDisabledRouteIds(List<Integer> ids)       { this.disabledRouteIds = ids; }
    public void setCostOverrides(Map<Integer, Double> m)     { this.costOverrides = m; }
    public void setCapacityOverrides(Map<Integer, Double> m) { this.capacityOverrides = m; }
    public void setSupplyMultiplier(double m)                { this.supplyMultiplier = m; }
    public void setDemandMultiplier(double m)                { this.demandMultiplier = m; }

    // ─── Inner value record ───────────────────────────────────────────────────

    public record ScenarioInputs(
            String               scenarioName,
            List<InventoryRecord> supplies,
            List<DemandRecord>    demands,
            List<TransportRoute>  routes
    ) {}
}
