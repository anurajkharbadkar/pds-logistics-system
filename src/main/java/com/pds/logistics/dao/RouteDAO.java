package com.pds.logistics.dao;

import com.pds.logistics.config.DatabaseConfig;
import com.pds.logistics.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for {@link TransportRoute} CRUD operations.
 */
public class RouteDAO {

    private static final Logger log = LoggerFactory.getLogger(RouteDAO.class);

    private final LocationDAO locationDAO = new LocationDAO();

    // ─── INSERT ───────────────────────────────────────────────────────────────

    public int insert(TransportRoute route) throws SQLException {
        String sql = """
                INSERT INTO transport_routes
                    (source_id, destination_id, capacity_mt, unit_cost_per_mt,
                     distance_km, transport_mode, active, remarks)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, route.getSource().getId());
            ps.setInt(2, route.getDestination().getId());
            ps.setDouble(3, route.getCapacityMT());
            ps.setDouble(4, route.getUnitCostPerMT());
            ps.setDouble(5, route.getDistanceKm());
            ps.setString(6, route.getTransportMode());
            ps.setBoolean(7, route.isActive());
            ps.setString(8, route.getRemarks());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int id = keys.getInt(1);
                route.setId(id);
                return id;
            }
        }
        return -1;
    }

    // ─── UPDATE ───────────────────────────────────────────────────────────────

    public void update(TransportRoute route) throws SQLException {
        String sql = """
                UPDATE transport_routes
                SET source_id=?, destination_id=?, capacity_mt=?, unit_cost_per_mt=?,
                    distance_km=?, transport_mode=?, active=?, remarks=?
                WHERE id=?
                """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, route.getSource().getId());
            ps.setInt(2, route.getDestination().getId());
            ps.setDouble(3, route.getCapacityMT());
            ps.setDouble(4, route.getUnitCostPerMT());
            ps.setDouble(5, route.getDistanceKm());
            ps.setString(6, route.getTransportMode());
            ps.setBoolean(7, route.isActive());
            ps.setString(8, route.getRemarks());
            ps.setInt(9, route.getId());
            ps.executeUpdate();
        }
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────

    public void delete(int id) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM transport_routes WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ─── SELECT ───────────────────────────────────────────────────────────────

    public List<TransportRoute> findAll() throws SQLException {
        String sql = """
                SELECT r.*, 
                       ls.id s_id, ls.code s_code, ls.name s_name, ls.type s_type,
                       ls.district s_dist, ls.state s_state, ls.latitude s_lat, ls.longitude s_lon,
                       ls.canvas_x s_cx, ls.canvas_y s_cy, ls.active s_active,
                       ld.id d_id, ld.code d_code, ld.name d_name, ld.type d_type,
                       ld.district d_dist, ld.state d_state, ld.latitude d_lat, ld.longitude d_lon,
                       ld.canvas_x d_cx, ld.canvas_y d_cy, ld.active d_active
                FROM transport_routes r
                JOIN locations ls ON r.source_id = ls.id
                JOIN locations ld ON r.destination_id = ld.id
                ORDER BY r.id
                """;
        List<TransportRoute> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<TransportRoute> findAllActive() throws SQLException {
        String sql = """
                SELECT r.*,
                       ls.id s_id, ls.code s_code, ls.name s_name, ls.type s_type,
                       ls.district s_dist, ls.state s_state, ls.latitude s_lat, ls.longitude s_lon,
                       ls.canvas_x s_cx, ls.canvas_y s_cy, ls.active s_active,
                       ld.id d_id, ld.code d_code, ld.name d_name, ld.type d_type,
                       ld.district d_dist, ld.state d_state, ld.latitude d_lat, ld.longitude d_lon,
                       ld.canvas_x d_cx, ld.canvas_y d_cy, ld.active d_active
                FROM transport_routes r
                JOIN locations ls ON r.source_id = ls.id
                JOIN locations ld ON r.destination_id = ld.id
                WHERE r.active = TRUE
                ORDER BY r.id
                """;
        List<TransportRoute> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // ─── Row Mapper ───────────────────────────────────────────────────────────

    private TransportRoute mapRow(ResultSet rs) throws SQLException {
        Location src = new Location();
        src.setId(rs.getInt("s_id"));
        src.setCode(rs.getString("s_code"));
        src.setName(rs.getString("s_name"));
        src.setType(LocationType.valueOf(rs.getString("s_type")));
        src.setDistrict(rs.getString("s_dist"));
        src.setState(rs.getString("s_state"));
        src.setLatitude(rs.getDouble("s_lat"));
        src.setLongitude(rs.getDouble("s_lon"));
        src.setCanvasX(rs.getDouble("s_cx"));
        src.setCanvasY(rs.getDouble("s_cy"));
        src.setActive(rs.getBoolean("s_active"));

        Location dst = new Location();
        dst.setId(rs.getInt("d_id"));
        dst.setCode(rs.getString("d_code"));
        dst.setName(rs.getString("d_name"));
        dst.setType(LocationType.valueOf(rs.getString("d_type")));
        dst.setDistrict(rs.getString("d_dist"));
        dst.setState(rs.getString("d_state"));
        dst.setLatitude(rs.getDouble("d_lat"));
        dst.setLongitude(rs.getDouble("d_lon"));
        dst.setCanvasX(rs.getDouble("d_cx"));
        dst.setCanvasY(rs.getDouble("d_cy"));
        dst.setActive(rs.getBoolean("d_active"));

        TransportRoute route = new TransportRoute();
        route.setId(rs.getInt("id"));
        route.setSource(src);
        route.setDestination(dst);
        route.setCapacityMT(rs.getDouble("capacity_mt"));
        route.setUnitCostPerMT(rs.getDouble("unit_cost_per_mt"));
        route.setDistanceKm(rs.getDouble("distance_km"));
        route.setTransportMode(rs.getString("transport_mode"));
        route.setActive(rs.getBoolean("active"));
        route.setRemarks(rs.getString("remarks"));
        return route;
    }
}
