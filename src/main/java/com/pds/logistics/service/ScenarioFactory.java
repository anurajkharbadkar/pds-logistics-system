package com.pds.logistics.service;

import com.pds.logistics.algorithm.Scenario;

import java.util.List;
import java.util.Map;

/**
 * Factory for creating pre-defined what-if scenarios.
 * All methods return fully configured {@link Scenario} objects ready to run.
 */
public class ScenarioFactory {

    /** Baseline: no modifications applied */
    public static Scenario createBaseline() {
        return new Scenario("Baseline", Scenario.ScenarioType.BASELINE,
                "Standard optimization with current supply, demand, and routes");
    }

    /**
     * Supply Shock: reduce all supply stocks by a given percentage.
     * @param reductionPct  e.g. 20.0 → 20% supply reduction
     */
    public static Scenario createSupplyShock(double reductionPct) {
        Scenario s = new Scenario(
                "Supply Shock -%.0f%%".formatted(reductionPct),
                Scenario.ScenarioType.SUPPLY_CHANGE,
                "Simulates %.0f%% reduction in all depot stocks".formatted(reductionPct)
        );
        s.setSupplyMultiplier(1.0 - reductionPct / 100.0);
        return s;
    }

    /**
     * Demand Surge: increase demand at all nodes by a given percentage.
     * @param surgePct  e.g. 30.0 → 30% demand increase
     */
    public static Scenario createDemandSurge(double surgePct) {
        Scenario s = new Scenario(
                "Demand Surge +%.0f%%".formatted(surgePct),
                Scenario.ScenarioType.DEMAND_SURGE,
                "Simulates %.0f%% increase in demand (seasonal/festival surge)".formatted(surgePct)
        );
        s.setDemandMultiplier(1.0 + surgePct / 100.0);
        return s;
    }

    /**
     * Route Failure: disables specific routes (e.g., road closure, flooding).
     * @param failedRouteIds  list of route IDs to disable
     */
    public static Scenario createRouteFailure(List<Integer> failedRouteIds) {
        Scenario s = new Scenario(
                "Route Failure (%d routes)".formatted(failedRouteIds.size()),
                Scenario.ScenarioType.ROUTE_FAILURE,
                "Simulates road/rail closure on %d route(s)".formatted(failedRouteIds.size())
        );
        s.setDisabledRouteIds(failedRouteIds);
        return s;
    }

    /**
     * Cost Change: modifies unit cost on specific routes.
     * @param costOverrides  map of routeId → new unit cost (₹/MT)
     */
    public static Scenario createCostChange(Map<Integer, Double> costOverrides) {
        Scenario s = new Scenario(
                "Cost Change (%d routes)".formatted(costOverrides.size()),
                Scenario.ScenarioType.COST_CHANGE,
                "Updates transport costs on %d route(s)".formatted(costOverrides.size())
        );
        s.setCostOverrides(costOverrides);
        return s;
    }

    /**
     * Capacity Change: modifies maximum capacity on specific routes.
     * @param capacityOverrides  map of routeId → new capacity in MT
     */
    public static Scenario createCapacityChange(Map<Integer, Double> capacityOverrides) {
        Scenario s = new Scenario(
                "Capacity Change (%d routes)".formatted(capacityOverrides.size()),
                Scenario.ScenarioType.CAPACITY_CHANGE,
                "Updates transport capacities on %d route(s)".formatted(capacityOverrides.size())
        );
        s.setCapacityOverrides(capacityOverrides);
        return s;
    }

    /**
     * Custom combined scenario with supply overrides per location.
     */
    public static Scenario createCustomSupply(Map<Integer, Double> supplyOverrides, String name) {
        Scenario s = new Scenario(name, Scenario.ScenarioType.SUPPLY_CHANGE,
                "Custom supply levels for selected depots/warehouses");
        s.setSupplyOverrides(supplyOverrides);
        return s;
    }

    private ScenarioFactory() {}
}
