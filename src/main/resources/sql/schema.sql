-- =============================================================================
-- PDS LOGISTICS OPTIMIZATION SYSTEM – MySQL 8.x Database Schema
-- File: schema.sql
-- Run: mysql -u root -p < schema.sql
-- =============================================================================

CREATE DATABASE IF NOT EXISTS pds_logistics
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE pds_logistics;

-- =============================================================================
-- 1. LOCATIONS
-- =============================================================================
CREATE TABLE IF NOT EXISTS locations (
    id          INT          AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(20)  NOT NULL UNIQUE COMMENT 'e.g. FCI-MH-01',
    name        VARCHAR(120) NOT NULL,
    type        ENUM('DEPOT','WAREHOUSE','DISTRIBUTION_CENTER','FAIR_PRICE_SHOP',
                     'SUPER_SOURCE','SUPER_SINK') NOT NULL,
    district    VARCHAR(80),
    state       VARCHAR(60)  NOT NULL DEFAULT 'Maharashtra',
    latitude    DOUBLE       NOT NULL DEFAULT 0.0,
    longitude   DOUBLE       NOT NULL DEFAULT 0.0,
    canvas_x    DOUBLE       NOT NULL DEFAULT 0.5 COMMENT 'Normalized [0,1] X for UI canvas',
    canvas_y    DOUBLE       NOT NULL DEFAULT 0.5 COMMENT 'Normalized [0,1] Y for UI canvas',
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_type   (type),
    INDEX idx_active (active)
) ENGINE=InnoDB COMMENT='All nodes in the PDS logistics network';

-- =============================================================================
-- 2. TRANSPORT ROUTES
-- =============================================================================
CREATE TABLE IF NOT EXISTS transport_routes (
    id                INT          AUTO_INCREMENT PRIMARY KEY,
    source_id         INT          NOT NULL,
    destination_id    INT          NOT NULL,
    capacity_mt       DOUBLE       NOT NULL DEFAULT 0 COMMENT 'Max metric tonnes per period',
    unit_cost_per_mt  DOUBLE       NOT NULL DEFAULT 0 COMMENT 'INR per metric tonne',
    distance_km       DOUBLE       NOT NULL DEFAULT 0,
    transport_mode    VARCHAR(10)  NOT NULL DEFAULT 'ROAD' COMMENT 'ROAD / RAIL / RIVER',
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    remarks           TEXT,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_route_source  FOREIGN KEY (source_id)      REFERENCES locations(id) ON DELETE CASCADE,
    CONSTRAINT fk_route_dest    FOREIGN KEY (destination_id)  REFERENCES locations(id) ON DELETE CASCADE,
    INDEX idx_src  (source_id),
    INDEX idx_dest (destination_id),
    INDEX idx_active_routes (active)
) ENGINE=InnoDB COMMENT='Directed transport edges with cost and capacity';

-- =============================================================================
-- 3. INVENTORY (Supply)
-- =============================================================================
CREATE TABLE IF NOT EXISTS inventory (
    id            INT         AUTO_INCREMENT PRIMARY KEY,
    location_id   INT         NOT NULL,
    grain_type    ENUM('WHEAT','RICE','COARSE_GRAINS','PULSES') NOT NULL,
    quantity_mt   DOUBLE      NOT NULL DEFAULT 0 COMMENT 'Total stock in metric tonnes',
    allocated_mt  DOUBLE      NOT NULL DEFAULT 0 COMMENT 'Already-reserved quantity',
    report_date   DATE,
    remarks       TEXT,
    created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_inv_loc FOREIGN KEY (location_id) REFERENCES locations(id) ON DELETE CASCADE,
    INDEX idx_inv_loc   (location_id),
    INDEX idx_inv_grain (grain_type)
) ENGINE=InnoDB COMMENT='Grain stock at each supply location';

-- =============================================================================
-- 4. DEMAND RECORDS
-- =============================================================================
CREATE TABLE IF NOT EXISTS demand_records (
    id                  INT         AUTO_INCREMENT PRIMARY KEY,
    location_id         INT         NOT NULL,
    grain_type          ENUM('WHEAT','RICE','COARSE_GRAINS','PULSES') NOT NULL,
    required_mt         DOUBLE      NOT NULL DEFAULT 0 COMMENT 'Required in current period (MT)',
    fulfilled_mt        DOUBLE      NOT NULL DEFAULT 0 COMMENT 'Filled by last optimization run',
    period_start        DATE,
    period_end          DATE,
    beneficiary_count   INT         NOT NULL DEFAULT 0 COMMENT 'Number of BPL/AAY households',
    remarks             TEXT,
    created_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_dem_loc FOREIGN KEY (location_id) REFERENCES locations(id) ON DELETE CASCADE,
    INDEX idx_dem_loc   (location_id),
    INDEX idx_dem_grain (grain_type)
) ENGINE=InnoDB COMMENT='Grain demand at each demand location per period';

-- =============================================================================
-- 5. OPTIMIZATION RUNS (History)
-- =============================================================================
CREATE TABLE IF NOT EXISTS optimization_runs (
    id                      INT          AUTO_INCREMENT PRIMARY KEY,
    scenario_name           VARCHAR(120) NOT NULL,
    grain_type              ENUM('WHEAT','RICE','COARSE_GRAINS','PULSES') NOT NULL,
    total_supply_mt         DOUBLE       NOT NULL DEFAULT 0,
    total_demand_mt         DOUBLE       NOT NULL DEFAULT 0,
    total_transported_mt    DOUBLE       NOT NULL DEFAULT 0,
    total_cost              DOUBLE       NOT NULL DEFAULT 0 COMMENT 'Total INR',
    fulfilled_demand_mt     DOUBLE       NOT NULL DEFAULT 0,
    unmet_demand_mt         DOUBLE       NOT NULL DEFAULT 0,
    network_efficiency_pct  DOUBLE       NOT NULL DEFAULT 0,
    solve_duration_ms       BIGINT       NOT NULL DEFAULT 0,
    algorithm_used          VARCHAR(60)  NOT NULL DEFAULT 'MCMF-SSP-Dijkstra',
    status                  VARCHAR(20)  NOT NULL DEFAULT 'SUCCESS' COMMENT 'SUCCESS/PARTIAL/INFEASIBLE',
    notes                   TEXT,
    run_timestamp           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_run_ts    (run_timestamp),
    INDEX idx_run_grain (grain_type),
    INDEX idx_run_status (status)
) ENGINE=InnoDB COMMENT='Summary header for each MCMF optimization run';

-- =============================================================================
-- 6. ALLOCATIONS (Per-route flow results)
-- =============================================================================
CREATE TABLE IF NOT EXISTS allocations (
    id                  INT     AUTO_INCREMENT PRIMARY KEY,
    run_id              INT     NOT NULL,
    route_id            INT     NOT NULL,
    grain_type          ENUM('WHEAT','RICE','COARSE_GRAINS','PULSES') NOT NULL,
    allocated_mt        DOUBLE  NOT NULL DEFAULT 0,
    cost_contribution   DOUBLE  NOT NULL DEFAULT 0 COMMENT 'allocated_mt × unit_cost',

    CONSTRAINT fk_alloc_run   FOREIGN KEY (run_id)   REFERENCES optimization_runs(id) ON DELETE CASCADE,
    CONSTRAINT fk_alloc_route FOREIGN KEY (route_id) REFERENCES transport_routes(id)  ON DELETE CASCADE,
    INDEX idx_alloc_run   (run_id),
    INDEX idx_alloc_route (route_id)
) ENGINE=InnoDB COMMENT='Individual route-level flow allocations per optimization run';
