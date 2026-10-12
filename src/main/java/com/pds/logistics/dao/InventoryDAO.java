package com.pds.logistics.dao;

import com.pds.logistics.config.DatabaseConfig;
import com.pds.logistics.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for {@link InventoryRecord} and {@link DemandRecord}.
 */
public class InventoryDAO {

    private static final Logger log = LoggerFactory.getLogger(InventoryDAO.class);

    private final LocationDAO locationDAO = new LocationDAO();

    // ─── INVENTORY ────────────────────────────────────────────────────────────

    public int insertInventory(InventoryRecord inv) throws SQLException {
        String sql = """
                INSERT INTO inventory (location_id, grain_type, quantity_mt,
                                       allocated_mt, report_date, remarks)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, inv.getLocation().getId());
            ps.setString(2, inv.getGrainType().name());
            ps.setDouble(3, inv.getQuantityMT());
            ps.setDouble(4, inv.getAllocatedMT());
            ps.setDate(5, inv.getReportDate() != null ? Date.valueOf(inv.getReportDate()) : null);
            ps.setString(6, inv.getRemarks());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) { inv.setId(keys.getInt(1)); return inv.getId(); }
        }
        return -1;
    }

    public void updateInventory(InventoryRecord inv) throws SQLException {
        String sql = "UPDATE inventory SET quantity_mt=?, allocated_mt=?, remarks=? WHERE id=?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, inv.getQuantityMT());
            ps.setDouble(2, inv.getAllocatedMT());
            ps.setString(3, inv.getRemarks());
            ps.setInt(4, inv.getId());
            ps.executeUpdate();
        }
    }

    public List<InventoryRecord> findInventoryByGrain(GrainType grain) throws SQLException {
        String sql = """
                SELECT i.*, l.id l_id, l.code l_code, l.name l_name, l.type l_type,
                       l.district l_dist, l.state l_state,
                       l.latitude l_lat, l.longitude l_lon,
                       l.canvas_x l_cx, l.canvas_y l_cy, l.active l_active
                FROM inventory i
                JOIN locations l ON i.location_id = l.id
                WHERE i.grain_type = ?
                ORDER BY l.type, l.name
                """;
        List<InventoryRecord> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, grain.name());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapInventoryRow(rs));
        }
        return list;
    }

    public List<InventoryRecord> findAllInventory() throws SQLException {
        String sql = """
                SELECT i.*, l.id l_id, l.code l_code, l.name l_name, l.type l_type,
                       l.district l_dist, l.state l_state,
                       l.latitude l_lat, l.longitude l_lon,
                       l.canvas_x l_cx, l.canvas_y l_cy, l.active l_active
                FROM inventory i
                JOIN locations l ON i.location_id = l.id
                ORDER BY l.type, l.name, i.grain_type
                """;
        List<InventoryRecord> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapInventoryRow(rs));
        }
        return list;
    }

    // ─── DEMAND ───────────────────────────────────────────────────────────────

    public int insertDemand(DemandRecord dem) throws SQLException {
        String sql = """
                INSERT INTO demand_records (location_id, grain_type, required_mt,
                                            fulfilled_mt, period_start, period_end,
                                            beneficiary_count, remarks)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, dem.getLocation().getId());
            ps.setString(2, dem.getGrainType().name());
            ps.setDouble(3, dem.getRequiredMT());
            ps.setDouble(4, dem.getFulfilledMT());
            ps.setDate(5, dem.getPeriodStart() != null ? Date.valueOf(dem.getPeriodStart()) : null);
            ps.setDate(6, dem.getPeriodEnd()   != null ? Date.valueOf(dem.getPeriodEnd())   : null);
            ps.setInt(7, dem.getBeneficiaryCount());
            ps.setString(8, dem.getRemarks());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) { dem.setId(keys.getInt(1)); return dem.getId(); }
        }
        return -1;
    }

    public List<DemandRecord> findDemandByGrain(GrainType grain) throws SQLException {
        String sql = """
                SELECT d.*, l.id l_id, l.code l_code, l.name l_name, l.type l_type,
                       l.district l_dist, l.state l_state,
                       l.latitude l_lat, l.longitude l_lon,
                       l.canvas_x l_cx, l.canvas_y l_cy, l.active l_active
                FROM demand_records d
                JOIN locations l ON d.location_id = l.id
                WHERE d.grain_type = ?
                ORDER BY l.name
                """;
        List<DemandRecord> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, grain.name());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapDemandRow(rs));
        }
        return list;
    }

    public List<DemandRecord> findAllDemands() throws SQLException {
        String sql = """
                SELECT d.*, l.id l_id, l.code l_code, l.name l_name, l.type l_type,
                       l.district l_dist, l.state l_state,
                       l.latitude l_lat, l.longitude l_lon,
                       l.canvas_x l_cx, l.canvas_y l_cy, l.active l_active
                FROM demand_records d
                JOIN locations l ON d.location_id = l.id
                ORDER BY l.name, d.grain_type
                """;
        List<DemandRecord> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapDemandRow(rs));
        }
        return list;
    }

    // ─── Row Mappers ──────────────────────────────────────────────────────────

    private InventoryRecord mapInventoryRow(ResultSet rs) throws SQLException {
        Location loc = buildLocation(rs);
        InventoryRecord inv = new InventoryRecord();
        inv.setId(rs.getInt("id"));
        inv.setLocation(loc);
        inv.setGrainType(GrainType.valueOf(rs.getString("grain_type")));
        inv.setQuantityMT(rs.getDouble("quantity_mt"));
        inv.setAllocatedMT(rs.getDouble("allocated_mt"));
        Date d = rs.getDate("report_date");
        if (d != null) inv.setReportDate(d.toLocalDate());
        inv.setRemarks(rs.getString("remarks"));
        return inv;
    }

    private DemandRecord mapDemandRow(ResultSet rs) throws SQLException {
        Location loc = buildLocation(rs);
        DemandRecord dem = new DemandRecord();
        dem.setId(rs.getInt("id"));
        dem.setLocation(loc);
        dem.setGrainType(GrainType.valueOf(rs.getString("grain_type")));
        dem.setRequiredMT(rs.getDouble("required_mt"));
        dem.setFulfilledMT(rs.getDouble("fulfilled_mt"));
        Date ps = rs.getDate("period_start");
        Date pe = rs.getDate("period_end");
        if (ps != null) dem.setPeriodStart(ps.toLocalDate());
        if (pe != null) dem.setPeriodEnd(pe.toLocalDate());
        dem.setBeneficiaryCount(rs.getInt("beneficiary_count"));
        dem.setRemarks(rs.getString("remarks"));
        return dem;
    }

    private Location buildLocation(ResultSet rs) throws SQLException {
        Location loc = new Location();
        loc.setId(rs.getInt("l_id"));
        loc.setCode(rs.getString("l_code"));
        loc.setName(rs.getString("l_name"));
        loc.setType(LocationType.valueOf(rs.getString("l_type")));
        loc.setDistrict(rs.getString("l_dist"));
        loc.setState(rs.getString("l_state"));
        loc.setLatitude(rs.getDouble("l_lat"));
        loc.setLongitude(rs.getDouble("l_lon"));
        loc.setCanvasX(rs.getDouble("l_cx"));
        loc.setCanvasY(rs.getDouble("l_cy"));
        loc.setActive(rs.getBoolean("l_active"));
        return loc;
    }
}
