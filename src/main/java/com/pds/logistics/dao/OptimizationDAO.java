package com.pds.logistics.dao;

import com.pds.logistics.config.DatabaseConfig;
import com.pds.logistics.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for persisting and retrieving {@link OptimizationResult} and
 * its child {@link AllocationResult} records.
 */
public class OptimizationDAO {

    private static final Logger log = LoggerFactory.getLogger(OptimizationDAO.class);

    private final RouteDAO routeDAO = new RouteDAO();

    /**
     * Saves a full optimization run (header + all allocations) in a single transaction.
     */
    public int saveResult(OptimizationResult result) throws SQLException {
        String headerSql = """
                INSERT INTO optimization_runs
                    (scenario_name, grain_type, total_supply_mt, total_demand_mt,
                     total_transported_mt, total_cost, fulfilled_demand_mt, unmet_demand_mt,
                     network_efficiency_pct, solve_duration_ms, algorithm_used, status, notes, run_timestamp)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        String allocSql = """
                INSERT INTO allocations
                    (run_id, route_id, grain_type, allocated_mt, cost_contribution)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int runId;
                try (PreparedStatement ps = conn.prepareStatement(headerSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, result.getScenarioName());
                    ps.setString(2, result.getGrainType().name());
                    ps.setDouble(3, result.getTotalSupplyMT());
                    ps.setDouble(4, result.getTotalDemandMT());
                    ps.setDouble(5, result.getTotalTransportedMT());
                    ps.setDouble(6, result.getTotalCost());
                    ps.setDouble(7, result.getFulfilledDemandMT());
                    ps.setDouble(8, result.getUnmetDemandMT());
                    ps.setDouble(9, result.getNetworkEfficiencyPct());
                    ps.setLong(10, result.getSolveDurationMs());
                    ps.setString(11, result.getAlgorithmUsed());
                    ps.setString(12, result.getStatus());
                    ps.setString(13, result.getNotes());
                    ps.setTimestamp(14, Timestamp.valueOf(result.getRunTimestamp()));
                    ps.executeUpdate();

                    ResultSet keys = ps.getGeneratedKeys();
                    keys.next();
                    runId = keys.getInt(1);
                    result.setId(runId);
                }

                try (PreparedStatement ps = conn.prepareStatement(allocSql)) {
                    for (AllocationResult alloc : result.getAllocations()) {
                        alloc.setOptimizationRunId(runId);
                        ps.setInt(1, runId);
                        ps.setInt(2, alloc.getRoute().getId());
                        ps.setString(3, alloc.getGrainType().name());
                        ps.setDouble(4, alloc.getAllocatedMT());
                        ps.setDouble(5, alloc.getCostContribution());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                conn.commit();
                log.info("Saved optimization run id={}", runId);
                return runId;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Returns a summary list of all optimization runs (without allocations).
     */
    public List<OptimizationResult> findAllRuns() throws SQLException {
        String sql = """
                SELECT * FROM optimization_runs ORDER BY run_timestamp DESC
                """;
        List<OptimizationResult> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapHeaderRow(rs));
        }
        return list;
    }

    /**
     * Loads a full result including all allocations.
     */
    public OptimizationResult findById(int id) throws SQLException {
        String sql = "SELECT * FROM optimization_runs WHERE id=?";
        OptimizationResult result = null;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) result = mapHeaderRow(rs);
        }
        if (result == null) return null;

        // Load allocations
        String allocSql = """
                SELECT a.*, r.source_id, r.destination_id, r.capacity_mt,
                       r.unit_cost_per_mt, r.distance_km, r.transport_mode, r.active, r.remarks
                FROM allocations a
                JOIN transport_routes r ON a.route_id = r.id
                WHERE a.run_id = ?
                """;
        List<TransportRoute> allRoutes = routeDAO.findAll();
        java.util.Map<Integer, TransportRoute> routeMap = new java.util.HashMap<>();
        allRoutes.forEach(r -> routeMap.put(r.getId(), r));

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(allocSql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int routeId = rs.getInt("route_id");
                TransportRoute route = routeMap.get(routeId);
                if (route == null) continue;
                AllocationResult alloc = new AllocationResult();
                alloc.setOptimizationRunId(id);
                alloc.setRoute(route);
                alloc.setAllocatedMT(rs.getDouble("allocated_mt"));
                alloc.setCostContribution(rs.getDouble("cost_contribution"));
                alloc.setGrainType(GrainType.valueOf(rs.getString("grain_type")));
                result.addAllocation(alloc);
            }
        }
        return result;
    }

    private OptimizationResult mapHeaderRow(ResultSet rs) throws SQLException {
        OptimizationResult r = new OptimizationResult();
        r.setId(rs.getInt("id"));
        r.setScenarioName(rs.getString("scenario_name"));
        r.setGrainType(GrainType.valueOf(rs.getString("grain_type")));
        r.setTotalSupplyMT(rs.getDouble("total_supply_mt"));
        r.setTotalDemandMT(rs.getDouble("total_demand_mt"));
        r.setTotalTransportedMT(rs.getDouble("total_transported_mt"));
        r.setTotalCost(rs.getDouble("total_cost"));
        r.setFulfilledDemandMT(rs.getDouble("fulfilled_demand_mt"));
        r.setUnmetDemandMT(rs.getDouble("unmet_demand_mt"));
        r.setNetworkEfficiencyPct(rs.getDouble("network_efficiency_pct"));
        r.setSolveDurationMs(rs.getLong("solve_duration_ms"));
        r.setAlgorithmUsed(rs.getString("algorithm_used"));
        r.setStatus(rs.getString("status"));
        r.setNotes(rs.getString("notes"));
        Timestamp ts = rs.getTimestamp("run_timestamp");
        if (ts != null) r.setRunTimestamp(ts.toLocalDateTime());
        return r;
    }
}
