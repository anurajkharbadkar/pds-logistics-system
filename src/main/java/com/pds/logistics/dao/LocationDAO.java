package com.pds.logistics.dao;

import com.pds.logistics.config.DatabaseConfig;
import com.pds.logistics.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for {@link Location} CRUD operations.
 */
public class LocationDAO {

    private static final Logger log = LoggerFactory.getLogger(LocationDAO.class);

    // ─── INSERT ───────────────────────────────────────────────────────────────

    public int insert(Location loc) throws SQLException {
        String sql = """
                INSERT INTO locations (code, name, type, district, state,
                                       latitude, longitude, canvas_x, canvas_y, active)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, loc.getCode());
            ps.setString(2, loc.getName());
            ps.setString(3, loc.getType().name());
            ps.setString(4, loc.getDistrict());
            ps.setString(5, loc.getState());
            ps.setDouble(6, loc.getLatitude());
            ps.setDouble(7, loc.getLongitude());
            ps.setDouble(8, loc.getCanvasX());
            ps.setDouble(9, loc.getCanvasY());
            ps.setBoolean(10, loc.isActive());

            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int id = keys.getInt(1);
                loc.setId(id);
                return id;
            }
        }
        return -1;
    }

    // ─── UPDATE ───────────────────────────────────────────────────────────────

    public void update(Location loc) throws SQLException {
        String sql = """
                UPDATE locations SET code=?, name=?, type=?, district=?, state=?,
                       latitude=?, longitude=?, canvas_x=?, canvas_y=?, active=?
                WHERE id=?
                """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, loc.getCode());
            ps.setString(2, loc.getName());
            ps.setString(3, loc.getType().name());
            ps.setString(4, loc.getDistrict());
            ps.setString(5, loc.getState());
            ps.setDouble(6, loc.getLatitude());
            ps.setDouble(7, loc.getLongitude());
            ps.setDouble(8, loc.getCanvasX());
            ps.setDouble(9, loc.getCanvasY());
            ps.setBoolean(10, loc.isActive());
            ps.setInt(11, loc.getId());
            ps.executeUpdate();
        }
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────

    public void delete(int id) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM locations WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ─── SELECT ───────────────────────────────────────────────────────────────

    public Location findById(int id) throws SQLException {
        String sql = "SELECT * FROM locations WHERE id=?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    public List<Location> findAll() throws SQLException {
        return findByActive(null);
    }

    public List<Location> findAllActive() throws SQLException {
        return findByActive(true);
    }

    public List<Location> findByType(LocationType type) throws SQLException {
        String sql = "SELECT * FROM locations WHERE type=? AND active=TRUE ORDER BY name";
        List<Location> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type.name());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    private List<Location> findByActive(Boolean active) throws SQLException {
        String sql = (active == null)
                ? "SELECT * FROM locations ORDER BY type, name"
                : "SELECT * FROM locations WHERE active=? ORDER BY type, name";

        List<Location> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (active != null) ps.setBoolean(1, active);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // ─── Row Mapper ───────────────────────────────────────────────────────────

    private Location mapRow(ResultSet rs) throws SQLException {
        Location loc = new Location();
        loc.setId(rs.getInt("id"));
        loc.setCode(rs.getString("code"));
        loc.setName(rs.getString("name"));
        loc.setType(LocationType.valueOf(rs.getString("type")));
        loc.setDistrict(rs.getString("district"));
        loc.setState(rs.getString("state"));
        loc.setLatitude(rs.getDouble("latitude"));
        loc.setLongitude(rs.getDouble("longitude"));
        loc.setCanvasX(rs.getDouble("canvas_x"));
        loc.setCanvasY(rs.getDouble("canvas_y"));
        loc.setActive(rs.getBoolean("active"));
        return loc;
    }
}
