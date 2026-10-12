package com.pds.logistics.service;

import com.pds.logistics.algorithm.*;
import com.pds.logistics.dao.*;
import com.pds.logistics.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

/**
 * Application service orchestrating the optimization workflow:
 * loads data from DB → applies scenario → runs MCMF → persists results.
 */
public class OptimizationService {

    private static final Logger log = LoggerFactory.getLogger(OptimizationService.class);

    private final LocationDAO     locationDAO     = new LocationDAO();
    private final RouteDAO        routeDAO        = new RouteDAO();
    private final InventoryDAO    inventoryDAO    = new InventoryDAO();
    private final OptimizationDAO optimizationDAO = new OptimizationDAO();

    /**
     * Runs optimization for the given grain type and scenario.
     * Loads live data from DB, applies scenario overrides, runs MCMF, persists result.
     *
     * @param grainType    grain commodity to optimize
     * @param scenario     scenario to apply (use BASELINE scenario for default run)
     * @param persistResult  whether to save result to DB
     * @return populated {@link OptimizationResult}
     */
    public OptimizationResult runOptimization(GrainType grainType,
                                               Scenario  scenario,
                                               boolean   persistResult) throws SQLException {

        log.info("OptimizationService: grain={}, scenario={}", grainType, scenario.getName());

        // 1. Load base data from DB
        List<InventoryRecord> baseSupplies = inventoryDAO.findInventoryByGrain(grainType);
        List<DemandRecord>    baseDemands  = inventoryDAO.findDemandByGrain(grainType);
        List<TransportRoute>  baseRoutes   = routeDAO.findAllActive();

        if (baseSupplies.isEmpty()) {
            log.warn("No supply records found for grain={}", grainType);
        }
        if (baseDemands.isEmpty()) {
            log.warn("No demand records found for grain={}", grainType);
        }

        // 2. Apply scenario modifications
        Scenario.ScenarioInputs inputs = scenario.apply(baseSupplies, baseDemands, baseRoutes);

        // 3. Run MCMF
        long start = System.currentTimeMillis();
        MCMFSolver solver = new MCMFSolver(
                inputs.supplies(),
                inputs.demands(),
                inputs.routes(),
                grainType,
                inputs.scenarioName()
        );

        OptimizationResult result = solver.solve();
        result.setSolveDurationMs(System.currentTimeMillis() - start);

        // 4. Persist result if requested
        if (persistResult) {
            try {
                optimizationDAO.saveResult(result);
                log.info("Persisted optimization run id={}", result.getId());
            } catch (SQLException e) {
                log.error("Failed to persist result: {}", e.getMessage());
                // Non-fatal: return result even if persistence fails
            }
        }

        return result;
    }

    /**
     * Runs the baseline (no modifications) optimization for all grain types.
     */
    public java.util.Map<GrainType, OptimizationResult> runAllGrains(boolean persist) throws SQLException {
        java.util.Map<GrainType, OptimizationResult> results = new java.util.LinkedHashMap<>();
        Scenario baseline = ScenarioFactory.createBaseline();
        for (GrainType grain : new GrainType[]{GrainType.WHEAT, GrainType.RICE}) {
            results.put(grain, runOptimization(grain, baseline, persist));
        }
        return results;
    }

    /** Returns all historical optimization runs. */
    public List<OptimizationResult> getOptimizationHistory() throws SQLException {
        return optimizationDAO.findAllRuns();
    }

    /** Returns a full result with allocations by ID. */
    public OptimizationResult getResultById(int id) throws SQLException {
        return optimizationDAO.findById(id);
    }

    // ─── Network Summary Helpers (for Dashboard) ──────────────────────────────

    public int countLocations() throws SQLException {
        return locationDAO.findAllActive().size();
    }

    public int countActiveRoutes() throws SQLException {
        return routeDAO.findAllActive().size();
    }

    public double getTotalWheatSupply() throws SQLException {
        return inventoryDAO.findInventoryByGrain(GrainType.WHEAT).stream()
                .mapToDouble(InventoryRecord::getEffectiveSupplyMT).sum();
    }

    public double getTotalRiceSupply() throws SQLException {
        return inventoryDAO.findInventoryByGrain(GrainType.RICE).stream()
                .mapToDouble(InventoryRecord::getEffectiveSupplyMT).sum();
    }

    public double getTotalWheatDemand() throws SQLException {
        return inventoryDAO.findDemandByGrain(GrainType.WHEAT).stream()
                .mapToDouble(DemandRecord::getRequiredMT).sum();
    }

    public double getTotalRiceDemand() throws SQLException {
        return inventoryDAO.findDemandByGrain(GrainType.RICE).stream()
                .mapToDouble(DemandRecord::getRequiredMT).sum();
    }
}
