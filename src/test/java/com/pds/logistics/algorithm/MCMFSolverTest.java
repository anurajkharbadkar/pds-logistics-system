package com.pds.logistics.algorithm;

import com.pds.logistics.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Min-Cost Max-Flow Solver Tests")
class MCMFSolverTest {

    private Location depot1;
    private Location depot2;
    private Location fps1;
    private Location fps2;

    @BeforeEach
    void setup() {
        depot1 = new Location(1, "DEP-01", "Central Depot", LocationType.DEPOT, "District 1", "State 1", 18.5, 73.8);
        depot1.setCanvasX(0.2); depot1.setCanvasY(0.2);

        depot2 = new Location(2, "DEP-02", "North Depot", LocationType.DEPOT, "District 2", "State 1", 19.1, 74.2);
        depot2.setCanvasX(0.2); depot2.setCanvasY(0.6);

        fps1 = new Location(3, "FPS-01", "Shop Alpha", LocationType.FAIR_PRICE_SHOP, "District 3", "State 1", 18.9, 74.8);
        fps1.setCanvasX(0.8); fps1.setCanvasY(0.3);

        fps2 = new Location(4, "FPS-02", "Shop Beta", LocationType.FAIR_PRICE_SHOP, "District 4", "State 1", 19.5, 75.1);
        fps2.setCanvasX(0.8); fps2.setCanvasY(0.7);
    }

    @Test
    @DisplayName("Single source and sink - optimal flow and cost calculation")
    void testSingleSourceSinkOptimalFlow() {
        LocalDate now = LocalDate.now();
        InventoryRecord inv = new InventoryRecord(1, depot1, GrainType.WHEAT, 100.0, now);
        DemandRecord dem = new DemandRecord(1, fps1, GrainType.WHEAT, 80.0, now, now.plusMonths(1), 500);

        TransportRoute route = new TransportRoute(1, depot1, fps1, 100.0, 25.0, 50.0, "ROAD");

        MCMFSolver solver = new MCMFSolver(
                List.of(inv),
                List.of(dem),
                List.of(route),
                GrainType.WHEAT,
                "Test Baseline"
        );

        OptimizationResult result = solver.solve();

        assertNotNull(result);
        assertEquals(80.0, result.getTotalTransportedMT(), 1e-6, "Should transport 80 MT to fulfill demand");
        assertEquals(2000.0, result.getTotalCost(), 1e-6, "Cost should be 80 MT * 25 ₹ = 2000 ₹");
        assertEquals(80.0, result.getFulfilledDemandMT(), 1e-6);
        assertEquals(0.0, result.getUnmetDemandMT(), 1e-6);
        assertEquals(100.0, result.getNetworkEfficiencyPct(), 1e-6);
        assertEquals(1, result.getAllocations().size());
        assertEquals(80.0, result.getAllocations().get(0).getAllocatedMT(), 1e-6);
    }

    @Test
    @DisplayName("Route capacity bottleneck restricts maximum transported quantity")
    void testCapacityBottleneck() {
        LocalDate now = LocalDate.now();
        InventoryRecord inv = new InventoryRecord(1, depot1, GrainType.RICE, 100.0, now);
        DemandRecord dem = new DemandRecord(1, fps1, GrainType.RICE, 80.0, now, now.plusMonths(1), 500);

        // Capacity is constrained to 45 MT
        TransportRoute route = new TransportRoute(1, depot1, fps1, 45.0, 20.0, 40.0, "ROAD");

        MCMFSolver solver = new MCMFSolver(
                List.of(inv),
                List.of(dem),
                List.of(route),
                GrainType.RICE,
                "Bottleneck Test"
        );

        OptimizationResult result = solver.solve();

        assertEquals(45.0, result.getTotalTransportedMT(), 1e-6);
        assertEquals(900.0, result.getTotalCost(), 1e-6, "45 MT * 20 ₹ = 900 ₹");
        assertEquals(45.0, result.getFulfilledDemandMT(), 1e-6);
        assertEquals(35.0, result.getUnmetDemandMT(), 1e-6, "80 - 45 = 35 MT unmet");
        assertEquals((45.0 / 80.0) * 100.0, result.getNetworkEfficiencyPct(), 1e-2);
    }

    @Test
    @DisplayName("Multi-depot multi-FPS chooses cheaper routes first")
    void testMinCostPreference() {
        LocalDate now = LocalDate.now();
        InventoryRecord inv1 = new InventoryRecord(1, depot1, GrainType.WHEAT, 100.0, now);
        InventoryRecord inv2 = new InventoryRecord(2, depot2, GrainType.WHEAT, 100.0, now);

        DemandRecord dem1 = new DemandRecord(1, fps1, GrainType.WHEAT, 50.0, now, now.plusMonths(1), 300);
        DemandRecord dem2 = new DemandRecord(2, fps2, GrainType.WHEAT, 50.0, now, now.plusMonths(1), 300);

        TransportRoute r1 = new TransportRoute(1, depot1, fps1, 100.0, 10.0, 20.0, "ROAD");
        TransportRoute r2 = new TransportRoute(2, depot1, fps2, 100.0, 50.0, 90.0, "ROAD");
        TransportRoute r3 = new TransportRoute(3, depot2, fps1, 100.0, 40.0, 80.0, "ROAD");
        TransportRoute r4 = new TransportRoute(4, depot2, fps2, 100.0, 15.0, 30.0, "ROAD");

        MCMFSolver solver = new MCMFSolver(
                List.of(inv1, inv2),
                List.of(dem1, dem2),
                List.of(r1, r2, r3, r4),
                GrainType.WHEAT,
                "Multi-node MinCost"
        );

        OptimizationResult result = solver.solve();

        assertEquals(100.0, result.getTotalTransportedMT(), 1e-6);
        assertEquals(1250.0, result.getTotalCost(), 1e-6, "Should choose cheapest routes (r1 and r4)");
        assertEquals(100.0, result.getFulfilledDemandMT(), 1e-6);
        assertEquals(0.0, result.getUnmetDemandMT(), 1e-6);
    }

    @Test
    @DisplayName("Inactive route is excluded from flow consideration")
    void testInactiveRouteExcluded() {
        LocalDate now = LocalDate.now();
        InventoryRecord inv = new InventoryRecord(1, depot1, GrainType.WHEAT, 100.0, now);
        DemandRecord dem = new DemandRecord(1, fps1, GrainType.WHEAT, 50.0, now, now.plusMonths(1), 200);

        TransportRoute route = new TransportRoute(1, depot1, fps1, 100.0, 10.0, 20.0, "ROAD");
        route.setActive(false); // Inactive

        MCMFSolver solver = new MCMFSolver(
                List.of(inv),
                List.of(dem),
                List.of(route),
                GrainType.WHEAT,
                "Inactive Route Test"
        );

        OptimizationResult result = solver.solve();

        assertEquals(0.0, result.getTotalTransportedMT(), 1e-6);
        assertEquals(0.0, result.getTotalCost(), 1e-6);
        assertEquals(50.0, result.getUnmetDemandMT(), 1e-6);
    }
}
