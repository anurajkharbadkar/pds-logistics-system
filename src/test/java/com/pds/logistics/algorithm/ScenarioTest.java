package com.pds.logistics.algorithm;

import com.pds.logistics.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("What-If Scenario Simulation Tests")
class ScenarioTest {

    private Location depot;
    private Location fps;
    private InventoryRecord inv;
    private DemandRecord dem;
    private TransportRoute route;

    @BeforeEach
    void setup() {
        depot = new Location(1, "DEP-1", "Central Depot", LocationType.DEPOT, "Dist", "State", 18.0, 73.0);
        fps = new Location(2, "FPS-1", "Shop 1", LocationType.FAIR_PRICE_SHOP, "Dist", "State", 18.5, 73.5);

        LocalDate now = LocalDate.now();
        inv = new InventoryRecord(1, depot, GrainType.WHEAT, 100.0, now);
        dem = new DemandRecord(1, fps, GrainType.WHEAT, 50.0, now, now.plusMonths(1), 200);
        route = new TransportRoute(10, depot, fps, 80.0, 20.0, 30.0, "ROAD");
    }

    @Test
    @DisplayName("Supply multiplier scales supply correctly without mutating base list")
    void testSupplyMultiplier() {
        Scenario scenario = new Scenario("20% Supply Drop", Scenario.ScenarioType.SUPPLY_CHANGE, "Drought impact");
        scenario.setSupplyMultiplier(0.8);

        Scenario.ScenarioInputs inputs = scenario.apply(List.of(inv), List.of(dem), List.of(route));

        assertEquals(80.0, inputs.supplies().get(0).getQuantityMT(), 1e-6);
        assertEquals(100.0, inv.getQuantityMT(), 1e-6, "Base supply list must remain unchanged");
    }

    @Test
    @DisplayName("Demand surge scales demand correctly")
    void testDemandMultiplier() {
        Scenario scenario = new Scenario("30% Demand Surge", Scenario.ScenarioType.DEMAND_SURGE, "Festival season");
        scenario.setDemandMultiplier(1.3);

        Scenario.ScenarioInputs inputs = scenario.apply(List.of(inv), List.of(dem), List.of(route));

        assertEquals(65.0, inputs.demands().get(0).getRequiredMT(), 1e-6);
        assertEquals(50.0, dem.getRequiredMT(), 1e-6, "Base demand list must remain unchanged");
    }

    @Test
    @DisplayName("Route failure disables specified routes")
    void testRouteFailure() {
        Scenario scenario = new Scenario("Bridge Collapse", Scenario.ScenarioType.ROUTE_FAILURE, "NH-48 closed");
        scenario.setDisabledRouteIds(List.of(10));

        Scenario.ScenarioInputs inputs = scenario.apply(List.of(inv), List.of(dem), List.of(route));

        assertFalse(inputs.routes().get(0).isActive(), "Disabled route must have active=false");
        assertTrue(route.isActive(), "Base route must remain active");
    }

    @Test
    @DisplayName("Cost override modifies specific route cost")
    void testCostOverride() {
        Scenario scenario = new Scenario("Fuel Price Hike", Scenario.ScenarioType.COST_CHANGE, "Diesel surge");
        scenario.setCostOverrides(Map.of(10, 35.0));

        Scenario.ScenarioInputs inputs = scenario.apply(List.of(inv), List.of(dem), List.of(route));

        assertEquals(35.0, inputs.routes().get(0).getUnitCostPerMT(), 1e-6);
        assertEquals(20.0, route.getUnitCostPerMT(), 1e-6);
    }
}
