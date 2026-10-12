package com.pds.logistics.model;

import java.util.Objects;

/**
 * Represents a node in the PDS logistics network.
 * Can be a supply depot, warehouse, distribution centre, or fair price shop.
 */
public class Location {

    private int id;
    private String code;          // e.g. "FCI-MH-01"
    private String name;
    private LocationType type;
    private String district;
    private String state;
    private double latitude;
    private double longitude;
    private boolean active;

    // Coordinates for canvas visualization (normalized 0..1)
    private double canvasX;
    private double canvasY;

    public Location() {}

    public Location(int id, String code, String name, LocationType type,
                    String district, String state,
                    double latitude, double longitude) {
        this.id        = id;
        this.code      = code;
        this.name      = name;
        this.type      = type;
        this.district  = district;
        this.state     = state;
        this.latitude  = latitude;
        this.longitude = longitude;
        this.active    = true;
    }

    // ─── Getters & Setters ────────────────────────────────────────────────────

    public int         getId()        { return id; }
    public void        setId(int id)  { this.id = id; }

    public String      getCode()              { return code; }
    public void        setCode(String code)   { this.code = code; }

    public String      getName()              { return name; }
    public void        setName(String name)   { this.name = name; }

    public LocationType getType()                    { return type; }
    public void         setType(LocationType type)   { this.type = type; }

    public String  getDistrict()               { return district; }
    public void    setDistrict(String district) { this.district = district; }

    public String  getState()             { return state; }
    public void    setState(String state) { this.state = state; }

    public double  getLatitude()                { return latitude; }
    public void    setLatitude(double latitude) { this.latitude = latitude; }

    public double  getLongitude()                 { return longitude; }
    public void    setLongitude(double longitude) { this.longitude = longitude; }

    public boolean isActive()                { return active; }
    public void    setActive(boolean active) { this.active = active; }

    public double  getCanvasX()              { return canvasX; }
    public void    setCanvasX(double canvasX){ this.canvasX = canvasX; }

    public double  getCanvasY()              { return canvasY; }
    public void    setCanvasY(double canvasY){ this.canvasY = canvasY; }

    // ─── Equality / Identity ─────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Location that)) return false;
        return id == that.id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "[%d] %s (%s) – %s".formatted(id, name, code, type.getDisplayName());
    }
}
